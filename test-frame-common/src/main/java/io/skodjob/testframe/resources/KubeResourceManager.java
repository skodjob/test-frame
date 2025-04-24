/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.resources;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.skodjob.testframe.TestFrameConstants;
import io.skodjob.testframe.TestFrameEnv;
import io.skodjob.testframe.clients.KubeClient;
import io.skodjob.testframe.clients.cmdClient.KubeCmdClient;
import io.skodjob.testframe.clients.cmdClient.Kubectl;
import io.skodjob.testframe.clients.cmdClient.Oc;
import io.skodjob.testframe.environment.TestEnvironmentVariables;
import io.skodjob.testframe.interfaces.ResourceType;
import io.skodjob.testframe.utils.LoggerUtils;
import io.skodjob.testframe.wait.Wait;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <h2>KubeResourceManager</h2>
 * Manages Kubernetes resources for testing purposes.
 *
 * <h3>Environment variable patterns</h3>
 * <pre>
 *   # default context (optional – falls back to ~/.kube/config current‑context)
 *   KUBE_URL     = https://api.dev:6443
 *   KUBE_TOKEN   = token
 *   KUBECONFIG   = /path/to/default.kubeconfig   # overrides URL/TOKEN
 *
 *   # extra contexts
 *   KUBECONFIG_PROD = /path/to/prod.kubeconfig  # highest precedence per context
 *   KUBE_URL_STAGE  = https://api.stage:6443
 *   KUBE_TOKEN_STAGE= token
 *   KUBE_URL_QA     = https://api.qa:6443
 *   KUBE_TOKEN_QA   = token
 * </pre>
 * <p>
 * The suffix after the final underscore becomes the context id (lower‑case).
 * The default context has no suffix.
 *
 * <h3>Switching contexts in tests</h3>
 *
 * <pre>
 * KubeResourceManager mgr = KubeResourceManager.get();
 * try (var ignored = mgr.useContext("prod")) {
 *     mgr.setTestContext(ctx);
 *     mgr.createResourceWithWait(myDeployment);
 * }
 * </pre>
 */
public final class KubeResourceManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(KubeResourceManager.class);

    private static final Map<String, TestEnvironmentVariables.ClusterConfig> CLUSTER_CONFIGS =
        TestFrameEnv.CLUSTER_CONFIGS;
    private String storeYamlPath;

    private final Map<String, ClusterContext> clientCache = new ConcurrentHashMap<>();
    private static final ThreadLocal<String> CURRENT_CLUSTER_CONTEXT = ThreadLocal.withInitial(() ->
        TestFrameConstants.DEFAULT_CONTEXT_NAME);
    private static final ThreadLocal<ExtensionContext> TEST_CONTEXT = new ThreadLocal<>();

    private static final KubeResourceManager INSTANCE = new KubeResourceManager();

    private volatile ResourceType<?>[] resourceTypes = new ResourceType<?>[]{};
    private final List<Consumer<HasMetadata>> createCallbacks = new CopyOnWriteArrayList<>();
    private final List<Consumer<HasMetadata>> deleteCallbacks = new CopyOnWriteArrayList<>();

    private static final Map<String, Map<String, Stack<ResourceItem<?>>>> STORED_RESOURCES = new ConcurrentHashMap<>();

    /**
     * Stores connected kube clients for context
     *
     * @param kubeClient kube client
     * @param cmdClient  cmd client
     */
    private record ClusterContext(KubeClient kubeClient, KubeCmdClient<?> cmdClient) {
    }

    private KubeResourceManager() {
        // Private constructor
    }

    /**
     * Gets KubeResourceManager instance
     *
     * @return singleton instance
     */
    public static KubeResourceManager get() {
        return INSTANCE;
    }

    /**
     * Gets KubeResourceManager instance
     *
     * @return singleton instance
     */
    @Deprecated(since = "0.9.0")
    public static KubeResourceManager getInstance() {
        return get();
    }

    /**
     * Set the active context for this thread and auto‑restore on close.
     *
     * @param id name of cluster context
     * @return context
     */
    public AutoCloseable useContext(String id) {
        String ctxId = Optional.ofNullable(id).orElse(TestFrameConstants.DEFAULT_CONTEXT_NAME).toLowerCase();
        if (!CLUSTER_CONFIGS.containsKey(ctxId)) {
            throw new IllegalArgumentException("Unknown context '" + ctxId +
                "'. Define env vars [KUBE_URL|KUBE_TOKEN|KUBECONFIG]_" + ctxId.toUpperCase());
        }
        LOGGER.info("Switching to context {}", ctxId);
        String prev = CURRENT_CLUSTER_CONTEXT.get();
        CURRENT_CLUSTER_CONTEXT.set(ctxId);
        return () -> {
            LOGGER.info("Closing context {}", ctxId);
            CURRENT_CLUSTER_CONTEXT.set(prev);
        };
    }

    /**
     * Creates context for cluster id and connect clients
     *
     * @param id id of cluster
     * @return context
     */
    private ClusterContext clusterContext(String id) {
        return clientCache.computeIfAbsent(id, cid -> {
            TestEnvironmentVariables.ClusterConfig c = CLUSTER_CONFIGS.get(cid);
            if (c == null) {
                throw new IllegalStateException("Credentials missing for context " + cid);
            }

            KubeClient kube;
            if (c.kubeconfigPath() != null) {
                kube = new KubeClient(c.kubeconfigPath());
            } else if (c.url() != null && c.token() != null) {
                kube = KubeClient.fromUrlAndToken(c.url(), c.token());
            } else {
                kube = new KubeClient();
            }

            KubeCmdClient<?> cmd = TestFrameEnv.CLIENT_TYPE.equals(TestFrameConstants.KUBERNETES_CLIENT)
                ? new Kubectl(kube.getKubeconfigPath())
                : new Oc(kube.getKubeconfigPath());
            return new ClusterContext(kube, cmd);
        });
    }

    /**
     * Gets current context
     *
     * @return context
     */
    private ClusterContext clusterContext() {
        return clusterContext(CURRENT_CLUSTER_CONTEXT.get());
    }

    /* ───────────────  kube clients accessors  ─────────────── */

    /**
     * Returns kube client for current context
     *
     * @return kube client
     */
    public KubeClient kubeClient() {
        return clusterContext().kubeClient;
    }

    /**
     * Returns kube cmd client for current context
     *
     * @return kube cmd client
     */
    public KubeCmdClient<?> kubeCmdClient() {
        return clusterContext().cmdClient;
    }

    /**
     * Set path for storing yaml resources
     *
     * @param path root path for storing
     */
    public void setStoreYamlPath(String path) {
        storeYamlPath = path;
    }

    /**
     * Returns root path of stored yaml resources
     *
     * @return path
     */
    public String getStoreYamlPath() {
        return storeYamlPath;
    }

    /**
     * Add resource types for special handling by resource manager
     *
     * @param types resource types implementation
     */
    public void setResourceTypes(ResourceType<?>... types) {
        this.resourceTypes = types;
    }

    /**
     * Adds callback which is called after every created resource
     *
     * @param cb callback
     */
    public void addCreateCallback(Consumer<HasMetadata> cb) {
        createCallbacks.add(cb);
    }

    /**
     * Adds delete callback which is called after every deletion of resource
     *
     * @param cb callback
     */
    public void addDeleteCallback(Consumer<HasMetadata> cb) {
        deleteCallbacks.add(cb);
    }

    /**
     * Sets test extension context
     *
     * @param ctx extension context
     */
    public void setTestContext(ExtensionContext ctx) {
        TEST_CONTEXT.set(ctx);
    }

    /**
     * Returns extension context for current test
     *
     * @return extension context
     */
    public ExtensionContext getTestContext() {
        return TEST_CONTEXT.get();
    }

    /**
     * Clean test extension context
     */
    public void cleanTestContext() {
        TEST_CONTEXT.remove();
    }

    /**
     * Pushes a resource to the stack.
     *
     * @param resource The resource to push.
     * @param <T>      The type of the resource.
     */
    public <T extends HasMetadata> void pushToStack(T resource) {
        STORED_RESOURCES
            .computeIfAbsent(CURRENT_CLUSTER_CONTEXT.get(), c -> new ConcurrentHashMap<>())
            .computeIfAbsent(getTestContext().getDisplayName(), t -> new Stack<>())
            .push(new ResourceItem<>(() -> deleteResource(resource), resource));
    }

    /**
     * Pushes a resource item to the stack.
     *
     * @param item The resource item to push.
     */
    public void pushToStack(ResourceItem<?> item) {
        STORED_RESOURCES
            .computeIfAbsent(CURRENT_CLUSTER_CONTEXT.get(), c -> new ConcurrentHashMap<>())
            .computeIfAbsent(getTestContext().getDisplayName(), t -> new Stack<>())
            .push(item);
    }

    /* ─────────────────────────  RESOURCE I/O HELPERS  ─────────────────────── */

    /**
     * Reads Kubernetes resources from a file at the specified path.
     *
     * @param file The path to the file containing Kubernetes resources.
     * @return A list of {@link HasMetadata} resources defined in the file.
     * @throws IOException If an I/O error occurs reading from the file.
     */
    public List<HasMetadata> readResourcesFromFile(Path file) throws IOException {
        return kubeClient().readResourcesFromFile(file);
    }

    /**
     * Reads Kubernetes resources from an InputStream.
     *
     * @param is The InputStream containing Kubernetes resources.
     * @return A list of {@link HasMetadata} resources defined in the stream.
     * @throws IOException If an I/O error occurs.
     */
    public List<HasMetadata> readResourcesFromFile(InputStream is) throws IOException {
        return kubeClient().readResourcesFromFile(is);
    }

    /* ───────────────────────────  LOGGING HELPERS  ─────────────────────────── */


    /**
     * Logs all managed resources across all test contexts with set log level
     *
     * @param logLevel slf4j log level event
     */
    public void printAllResources(Level logLevel) {
        LOGGER.atLevel(logLevel).log("Printing all managed resources across all contexts");
        STORED_RESOURCES.forEach((ctxId, byTest) -> {
            LOGGER.atLevel(logLevel).log("Context [{}]", ctxId);
            byTest.forEach((test, stack) -> {
                LOGGER.atLevel(logLevel).log("  Test: {}", test);
                stack.forEach(item -> Optional.ofNullable(item.resource())
                    .ifPresent(r -> LoggerUtils.logResource("Managed resource:", logLevel, r)));
            });
        });
    }

    /**
     * Logs all managed resources in current test context with set log level
     *
     * @param logLevel slf4j log level event
     */
    public void printCurrentResources(Level logLevel) {
        String ctxId = CURRENT_CLUSTER_CONTEXT.get();
        String test = getTestContext().getDisplayName();
        LOGGER.atLevel(logLevel).log("Resources in [{}]/{}", ctxId, test);
        Optional.ofNullable(STORED_RESOURCES.get(ctxId))
            .map(m -> m.get(test))
            .ifPresent(stack -> stack.forEach(i ->
                Optional.ofNullable(i.resource()).ifPresent(r ->
                    LoggerUtils.logResource("Managed resource:", logLevel, r))));
    }

    /* ──────────────────  CREATE / UPDATE / DELETE IMPLEMENTATION  ─────────── */

    // ---------------------------  Resource create  ---------------------------

    /**
     * Creates resources without waiting for readiness.
     *
     * @param resources The resources to create.
     * @param <T>       The type of the resources.
     */
    @SafeVarargs
    public final <T extends HasMetadata> void createResourceWithoutWait(T... resources) {
        createOrUpdateResource(false, false, false, resources);
    }

    /**
     * Creates resources and waits for readiness.
     *
     * @param resources The resources to create.
     * @param <T>       The type of the resources.
     */
    @SafeVarargs
    public final <T extends HasMetadata> void createResourceWithWait(T... resources) {
        createOrUpdateResource(false, true, false, resources);
    }

    /**
     * Creates or updates resources and waits for readiness.
     *
     * @param resources The resources to create.
     * @param <T>       The type of the resources.
     */
    @SafeVarargs
    public final <T extends HasMetadata> void createOrUpdateResourceWithWait(T... resources) {
        createOrUpdateResource(false, true, true, resources);
    }

    /**
     * Creates or updates resources.
     *
     * @param resources The resources to create.
     * @param <T>       The type of the resources.
     */
    @SafeVarargs
    public final <T extends HasMetadata> void createOrUpdateResourceWithoutWait(T... resources) {
        createOrUpdateResource(false, false, true, resources);
    }

    /**
     * Creates resources and wait on the end for all readiness.
     *
     * @param resources The resources to create.
     * @param <T>       The type of the resources.
     */
    @SafeVarargs
    public final <T extends HasMetadata> void createResourceAsyncWait(T... resources) {
        createOrUpdateResource(true, true, false, resources);
    }

    /**
     * Creates or updates resources and wait on the end for all readiness.
     *
     * @param resources The resources to create.
     * @param <T>       The type of the resources.
     */
    @SafeVarargs
    public final <T extends HasMetadata> void createOrUpdateResourceAsyncWait(T... resources) {
        createOrUpdateResource(true, true, true, resources);
    }

    /**
     * Creates resources with or without waiting for readiness.
     *
     * @param async       Flag waiting for all resources on the end
     * @param waitReady   Flag indicating whether to wait for readiness.
     * @param allowUpdate Flag indicating if update resource is allowed
     * @param resources   The resources to create.
     * @param <T>         The type of the resources.
     */
    @SafeVarargs
    private <T extends HasMetadata> void createOrUpdateResource(
        boolean async, boolean waitReady, boolean allowUpdate, T... resources) {
        List<CompletableFuture<Void>> waiters = new ArrayList<>();
        for (T resource : resources) {
            ResourceType<T> type = findResourceType(resource);
            pushToStack(resource);
            if (storeYamlPath != null) {
                writeResourceAsYaml(resource);
            }

            if (type == null) {
                if (allowUpdate && kubeClient().getClient().resource(resource).get() != null) {
                    LoggerUtils.logResource("Updating", resource);
                    kubeClient().getClient().resource(resource).update();
                } else {
                    LoggerUtils.logResource("Creating", resource);
                    kubeClient().getClient().resource(resource).create();
                }
                if (waitReady) {
                    CompletableFuture<Void> cf = CompletableFuture.runAsync(() ->
                        assertTrue(waitResourceCondition(resource,
                                new ResourceCondition<>(p -> {
                                    if (isResourceWithReadiness(resource)) {
                                        return kubeClient().getClient().resource(resource).isReady();
                                    }
                                    return kubeClient().getClient().resource(resource) != null;
                                }, "ready")),
                            "Timed out waiting for " + resource.getKind() + "/" +
                                resource.getMetadata().getName()));
                    if (async) {
                        waiters.add(cf);
                    } else {
                        cf.join();
                    }
                }
            } else {
                if (allowUpdate && kubeClient().getClient().resource(resource).get() != null) {
                    LoggerUtils.logResource("Updating", resource);
                    type.update(resource);
                } else {
                    LoggerUtils.logResource("Creating", resource);
                    type.create(resource);
                }
                if (waitReady) {
                    long timeout = Objects.requireNonNullElse(type.getTimeoutForResourceReadiness(),
                        TestFrameConstants.GLOBAL_TIMEOUT_MEDIUM);
                    CompletableFuture<Void> cf = CompletableFuture.runAsync(() ->
                        assertTrue(waitResourceCondition(resource, ResourceCondition.readiness(type), timeout),
                            "Timed out waiting for " + resource.getKind() + "/" +
                                resource.getMetadata().getName()));
                    if (async) {
                        waiters.add(cf);
                    } else {
                        cf.join();
                    }
                }
            }
            createCallbacks.forEach(cb -> cb.accept(resource));
        }
        if (!waiters.isEmpty()) {
            CompletableFuture.allOf(waiters.toArray(new CompletableFuture[0])).join();
        }
    }

    /**
     * Deletes resources.
     *
     * @param resources The resources to delete.
     * @param <T>       The type of the resources.
     */
    @SafeVarargs
    public final <T extends HasMetadata> void deleteResource(T... resources) {
        deleteResource(true, resources);
    }

    /**
     * Deletes resources.
     *
     * @param async     Enables async deletion
     * @param resources The resources to delete.
     * @param <T>       The type of the resources.
     */
    @SafeVarargs
    public final <T extends HasMetadata> void deleteResource(boolean async, T... resources) {
        List<CompletableFuture<Void>> waiters = new ArrayList<>();
        for (T resource : resources) {
            ResourceType<T> type = findResourceType(resource);
            LoggerUtils.logResource("Deleting", resource);
            try {
                if (type == null) {
                    kubeClient().getClient().resource(resource).delete();
                } else {
                    type.delete(resource);
                }
                decideDeleteWaitAsync(waiters, async, resource);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
            deleteCallbacks.forEach(cb -> cb.accept(resource));
        }
        if (!waiters.isEmpty()) {
            CompletableFuture.allOf(waiters.toArray(new CompletableFuture[0])).join();
        }
    }

    /**
     * Updates resources.
     *
     * @param resources The resources to update.
     * @param <T>       The type of the resources.
     */
    @SafeVarargs
    public final <T extends HasMetadata> void updateResource(T... resources) {
        for (T resource : resources) {
            LoggerUtils.logResource("Updating", resource);
            ResourceType<T> type = findResourceType(resource);
            if (type != null) {
                type.update(resource);
            } else {
                kubeClient().getClient().resource(resource).update();
            }
        }
    }

    /**
     * Method for replacing the resource with retries.
     * It uses {@link #replaceResource(HasMetadata, Consumer)} in try-catch block and in case
     * that the exception is thrown, it checks if we got conflict exception (meaning that we
     * should re-apply the changes on updated resource).
     * Otherwise, the {@link RuntimeException} is thrown (as we are not in conflict and there
     * is something else).
     * This encapsulates {@link #replaceResourceWithRetries(HasMetadata, Consumer, int)}
     * where the default number of retries is 3.
     *
     * @param resource The resource that should be updated.
     * @param editor   Editor containing all changes that should be propagated to resource
     * @param <T>      The type of the resource.
     */
    public <T extends HasMetadata> void replaceResourceWithRetries(T resource, Consumer<T> editor) {
        replaceResourceWithRetries(resource, editor, 3);
    }

    /**
     * Method for replacing the resource with retries.
     * It uses {@link #replaceResource(HasMetadata, Consumer)} in try-catch block and in case
     * that the exception is thrown, it checks if we got conflict exception (meaning that we
     * should re-apply the changes on updated resource).
     * Otherwise, the {@link RuntimeException} is thrown (as we are not in conflict and there
     * is something else).
     * This is retried for number of times specified in {@param retries}.
     *
     * @param resource The resource that should be updated.
     * @param editor   Editor containing all changes that should be propagated to resource
     * @param retries  Number of retries with which we should try to replace the resource
     * @param <T>      The type of the resource.
     */
    public <T extends HasMetadata> void replaceResourceWithRetries(T resource, Consumer<T> editor, int retries) {
        int attempt = 0;
        while (true) {
            try {
                replaceResource(resource, editor);
                return;
            } catch (CompletionException ce) {
                Throwable cause = ce.getCause();
                if (!isConflict(cause) || ++attempt >= retries) {
                    throw (cause instanceof RuntimeException re) ? re : new RuntimeException(cause);
                }
            } catch (KubernetesClientException kce) {
                if (!isConflict(kce) || ++attempt >= retries) {
                    throw kce;
                }
            }
        }
    }

    /**
     * Checks if the {@link Throwable} is instance of {@link KubernetesClientException}
     * and if the code is 409 - which means that we got conflict exception during operation.
     *
     * @param t throwable thrown during operation
     * @return boolean value if we got conflict during K8s operation or not
     */
    private static boolean isConflict(Throwable t) {
        return t instanceof KubernetesClientException kce && kce.getCode() == 409;
    }

    /**
     * Based on {@param resource} and {@param editor} replaces the current resource.
     * In case that the {@link ResourceType} is not found, the default client is used.
     *
     * @param resource The resource that should be updated.
     * @param editor   Editor containing all changes that should be propagated to resource
     * @param <T>      The type of the resource.
     */
    public <T extends HasMetadata> void replaceResource(T resource, Consumer<T> editor) {
        ResourceType<T> type = findResourceType(resource);
        if (type != null) {
            type.replace(resource, editor);
        } else {
            T current = kubeClient().getClient().resource(resource).get();
            editor.accept(current);
            kubeClient().getClient().resource(current).update();
        }
    }

    // ---------------------------  Wait condition -----------------------------

    /**
     * Waits for a resource condition to be fulfilled.
     *
     * @param resource  The resource to wait for.
     * @param condition The condition to fulfill.
     * @param <T>       The type of the resource.
     * @return True if the condition is fulfilled, false otherwise.
     */
    public <T extends HasMetadata> boolean waitResourceCondition(T resource, ResourceCondition<T> condition) {
        return waitResourceCondition(resource, condition, TestFrameConstants.GLOBAL_TIMEOUT);
    }

    /**
     * Waits for a resource condition to be fulfilled.
     *
     * @param resource        The resource to wait for.
     * @param condition       The condition to fulfill.
     * @param <T>             The type of the resource.
     * @param resourceTimeout Timeout for resource condition
     * @return True if the condition is fulfilled, false otherwise.
     */
    public <T extends HasMetadata> boolean waitResourceCondition(
        T resource, ResourceCondition<T> condition, long resourceTimeout) {
        assertNotNull(resource);
        assertNotNull(resource.getMetadata());
        assertNotNull(resource.getMetadata().getName());
        boolean[] ready = new boolean[1];
        Wait.until("Condition " + condition.conditionName(),
            TestFrameConstants.GLOBAL_POLL_INTERVAL_MEDIUM, resourceTimeout, () -> {
                T r = kubeClient().getClient().resource(resource).get();
                ready[0] = condition.predicate().test(r);
                return ready[0];
            });
        return ready[0];
    }

    /* --------------------------  DELETE ALL RESOURCES ----------------------- */

    /**
     * Deletes all stored resources.
     */
    public void deleteResources() {
        deleteResources(true);
    }

    /**
     * Deletes all stored resources.
     *
     * @param async sets async or sequential deletion
     */
    public void deleteResources(boolean async) {
        LoggerUtils.logSeparator();
        String ctxId = CURRENT_CLUSTER_CONTEXT.get();
        String testName = getTestContext().getDisplayName();
        Map<String, Stack<ResourceItem<?>>> byTest = STORED_RESOURCES.get(ctxId);
        if (byTest == null || byTest.get(testName) == null || byTest.get(testName).isEmpty()) {
            LOGGER.info("No resources to delete for [{}]/{}", ctxId, testName);
            return;
        }
        LOGGER.info("Deleting all resources for [{}]/{}", ctxId, testName);
        Stack<ResourceItem<?>> stack = byTest.get(testName);
        AtomicInteger count = new AtomicInteger(stack.size());
        while (!stack.isEmpty()) {
            ResourceItem<?> item = stack.pop();
            List<CompletableFuture<Void>> waiters = new ArrayList<>();
            CompletableFuture<Void> cf = CompletableFuture.runAsync(() -> {
                try {
                    item.throwableRunner().run();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            if (async) {
                waiters.add(cf);
            } else {
                cf.join();
            }
            count.decrementAndGet();
            deleteCallbacks.forEach(cb -> Optional.ofNullable(item.resource()).ifPresent(cb));
            if (!waiters.isEmpty()) {
                CompletableFuture.allOf(waiters.toArray(new CompletableFuture[0])).join();
            }
        }
        byTest.remove(testName);
        if (byTest.isEmpty()) {
            STORED_RESOURCES.remove(ctxId);
        }
        LoggerUtils.logSeparator();
    }

    /**
     * Return ResourceType implementation if it is specified in resourceTypes based on kind
     *
     * @param resource HasMetadata resource to find
     * @param <T>      The type of the resource.
     * @return {@link ResourceType}
     */
    @SuppressWarnings("unchecked")
    private <T extends HasMetadata> ResourceType<T> findResourceType(T resource) {
        for (ResourceType<?> rt : resourceTypes) {
            if (rt.getKind().equals(resource.getKind())) {
                return (ResourceType<T>) rt;
            }
        }
        return null;
    }

    private <T extends HasMetadata> boolean isResourceWithReadiness(T resource) {
        return resource instanceof Deployment ||
            resource instanceof io.fabric8.kubernetes.api.model.extensions.Deployment ||
            resource instanceof ReplicaSet ||
            resource instanceof Pod ||
            resource instanceof ReplicationController ||
            resource instanceof Endpoints ||
            resource instanceof Node ||
            resource instanceof StatefulSet;
    }

    private void writeResourceAsYaml(HasMetadata res) {
        File dir = Paths.get(storeYamlPath).resolve(CURRENT_CLUSTER_CONTEXT.get()).resolve("test-files")
            .resolve(getTestContext().getRequiredTestClass().getName())
            .toFile();
        if (getTestContext().getTestMethod().isPresent()) {
            dir = dir.toPath().resolve(getTestContext().getRequiredTestMethod().getName()).toFile();
        }
        if (!dir.exists() && !dir.mkdirs()) {
            throw new RuntimeException("Cannot create dir " + dir);
        }
        String yaml = Serialization.asYaml(res);
        try {
            Files.writeString(dir.toPath().resolve(res.getKind() + "-" +
                (res.getMetadata().getNamespace() == null ? "" : res.getMetadata().getNamespace() + "-") +
                res.getMetadata().getName() + ".yaml"), yaml, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private <T extends HasMetadata> void decideDeleteWaitAsync(
        List<CompletableFuture<Void>> waiters, boolean async, T res) {
        CompletableFuture<Void> cf = CompletableFuture.runAsync(() ->
            assertTrue(waitResourceCondition(res, ResourceCondition.deletion()),
                "Timed out deleting " + res.getKind() + "/" + res.getMetadata().getName()));
        if (async) {
            waiters.add(cf);
        } else {
            cf.join();
        }
    }
}
