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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.CompletableFuture;
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
import io.fabric8.kubernetes.client.utils.Serialization;
import io.skodjob.testframe.utils.LoggerUtils;
import io.skodjob.testframe.TestFrameConstants;
import io.skodjob.testframe.TestFrameEnv;
import io.skodjob.testframe.clients.KubeClient;
import io.skodjob.testframe.clients.cmdClient.KubeCmdClient;
import io.skodjob.testframe.clients.cmdClient.Kubectl;
import io.skodjob.testframe.clients.cmdClient.Oc;
import io.skodjob.testframe.interfaces.ResourceType;
import io.skodjob.testframe.wait.Wait;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Manages Kubernetes resources for testing purposes.
 */
public class KubeResourceManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(KubeResourceManager.class);

    private static KubeResourceManager instance;
    private static KubeClient client;
    private static KubeCmdClient<?> kubeCmdClient;
    private ResourceType<?>[] resourceTypes;
    private final List<Consumer<HasMetadata>> createCallbacks = new LinkedList<>();
    private final List<Consumer<HasMetadata>> deleteCallbacks = new LinkedList<>();

    private static final ThreadLocal<ExtensionContext> TEST_CONTEXT = new ThreadLocal<>();
    private static final Map<String, Stack<ResourceItem<?>>> STORED_RESOURCES = new LinkedHashMap<>();

    private static String storeYamlPath = null;

    private KubeResourceManager() {
        // Private constructor to prevent instantiation
    }

    /**
     * Retrieves the singleton instance of KubeResourceManager.
     *
     * @return The singleton instance of KubeResourceManager.
     */
    public static synchronized KubeResourceManager getInstance() {
        if (instance == null) {
            instance = new KubeResourceManager();
            instance.resourceTypes = new ResourceType[]{};
            client = new KubeClient();
            if (TestFrameEnv.CLIENT_TYPE.equals(TestFrameConstants.KUBERNETES_CLIENT)) {
                kubeCmdClient = new Kubectl(client.getKubeconfigPath());
            } else {
                kubeCmdClient = new Oc(client.getKubeconfigPath());
            }
        }
        return instance;
    }

    /**
     * Retrieves the Kubernetes client.
     *
     * @return The Kubernetes client.
     */
    public static KubeClient getKubeClient() {
        return client;
    }

    /**
     * Retrieves the Kubernetes command-line client.
     *
     * @return The Kubernetes command-line client.
     */
    public static KubeCmdClient<?> getKubeCmdClient() {
        return kubeCmdClient;
    }

    /**
     * Sets the test context.
     *
     * @param context The extension context.
     */
    public static void setTestContext(ExtensionContext context) {
        TEST_CONTEXT.set(context);
    }

    /**
     * Retrieves the test context.
     *
     * @return The extension context.
     */
    public static ExtensionContext getTestContext() {
        return TEST_CONTEXT.get();
    }

    /**
     * Sets the resource types.
     *
     * @param types The resource types implementing {@link ResourceType}
     */
    public final void setResourceTypes(ResourceType<?>... types) {
        this.resourceTypes = types;
    }

    /**
     * Add callback function which is called after creation of every resource
     *
     * @param callback function
     */
    public final void addCreateCallback(Consumer<HasMetadata> callback) {
        this.createCallbacks.add(callback);
    }

    /**
     * Add callback function which is called after deletion of every resource
     *
     * @param callback function
     */
    public final void addDeleteCallback(Consumer<HasMetadata> callback) {
        this.deleteCallbacks.add(callback);
    }

    /**
     * Set path for storing yaml resources
     *
     * @param path root path for storing
     */
    public static void setStoreYamlPath(String path) {
        storeYamlPath = path;
    }

    /**
     * Returns root path of stored yaml resources
     *
     * @return path
     */
    public static String getStoreYamlPath() {
        return storeYamlPath;
    }

    /**
     * Reads Kubernetes resources from a file at the specified path.
     *
     * @param file The path to the file containing Kubernetes resources.
     * @return A list of {@link HasMetadata} resources defined in the file.
     * @throws IOException If an I/O error occurs reading from the file.
     */
    public List<HasMetadata> readResourcesFromFile(Path file) throws IOException {
        return client.readResourcesFromFile(file);
    }

    /**
     * Reads Kubernetes resources from an InputStream.
     *
     * @param is The InputStream containing Kubernetes resources.
     * @return A list of {@link HasMetadata} resources defined in the stream.
     * @throws IOException If an I/O error occurs.
     */
    public List<HasMetadata> readResourcesFromFile(InputStream is) throws IOException {
        return client.readResourcesFromFile(is);
    }

    /**
     * Pushes a resource item to the stack.
     *
     * @param item The resource item to push.
     */
    public final void pushToStack(ResourceItem<?> item) {
        synchronized (this) {
            STORED_RESOURCES.computeIfAbsent(getTestContext().getDisplayName(), k -> new Stack<>());
            STORED_RESOURCES.get(getTestContext().getDisplayName()).push(item);
        }
    }

    /**
     * Pushes a resource to the stack.
     *
     * @param resource The resource to push.
     * @param <T>      The type of the resource.
     */
    public final <T extends HasMetadata> void pushToStack(T resource) {
        synchronized (this) {
            STORED_RESOURCES.computeIfAbsent(getTestContext().getDisplayName(), k -> new Stack<>());
            STORED_RESOURCES.get(getTestContext().getDisplayName()).push(
                new ResourceItem<T>(
                    () -> deleteResource(resource),
                    resource
                ));
        }
    }

    /**
     * Logs all managed resources across all test contexts with set log level
     *
     * @param logLevel slf4j log level event
     */
    public void printAllResources(Level logLevel) {
        LOGGER.atLevel(logLevel).log("Printing all managed resources from all test contexts");
        STORED_RESOURCES.forEach((testName, resources) -> {
            LOGGER.atLevel(logLevel).log("Context: {}", testName);
            resources.forEach(resourceItem -> {
                if (resourceItem.resource() != null) {
                    LoggerUtils.logResource("Managed resource:", logLevel, resourceItem.resource());
                }
            });
        });
    }

    /**
     * Logs all managed resources in current test context with set log level
     *
     * @param logLevel slf4j log level event
     */
    public void printCurrentResources(Level logLevel) {
        LOGGER.atLevel(logLevel).log("Printing all managed resources from current test context");
        STORED_RESOURCES.get(getTestContext().getDisplayName()).forEach(resourceItem -> {
            if (resourceItem.resource() != null) {
                LoggerUtils.logResource("Managed resource:", logLevel, resourceItem.resource());
            }
        });
    }

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
    private <T extends HasMetadata> void createOrUpdateResource(boolean async,
                                                                boolean waitReady,
                                                                boolean allowUpdate,
                                                                T... resources) {
        List<CompletableFuture<Void>> waitExecutors = new LinkedList<>();
        for (T resource : resources) {
            ResourceType<T> type = findResourceType(resource);
            pushToStack(resource);
            if (storeYamlPath != null) {
                writeResourceAsYaml(resource);
            }

            if (type == null) {
                // Generic create for any resource
                if (allowUpdate && client.getClient().resource(resource).get() != null) {
                    LoggerUtils.logResource("Updating", resource);
                    client.getClient().resource(resource).update();
                } else {
                    LoggerUtils.logResource("Creating", resource);
                    client.getClient().resource(resource).create();
                }
                if (waitReady) {
                    CompletableFuture<Void> c = CompletableFuture.runAsync(() ->
                        assertTrue(waitResourceCondition(resource,
                                new ResourceCondition<>(p -> {
                                    if (isResourceWithReadiness(resource)) {
                                        return client.getClient().resource(resource).isReady();
                                    }
                                    return client.getClient().resource(resource) != null;
                                }, "ready")),
                            String.format("Timed out waiting for %s/%s in %s to be ready", resource.getKind(),
                                resource.getMetadata().getName(), resource.getMetadata().getNamespace())));
                    if (async) {
                        waitExecutors.add(c);
                    } else {
                        CompletableFuture.allOf(c).join();
                    }
                }
            } else {
                // Create for typed resource implementing ResourceType
                if (allowUpdate && client.getClient().resource(resource).get() != null) {
                    LoggerUtils.logResource("Updating", resource);
                    type.update(resource);
                } else {
                    LoggerUtils.logResource("Creating", resource);
                    type.create(resource);
                }
                if (waitReady) {
                    CompletableFuture<Void> c = CompletableFuture.runAsync(() ->
                        assertTrue(waitResourceCondition(resource, ResourceCondition.readiness(type)),
                            String.format("Timed out waiting for %s/%s in %s to be ready", resource.getKind(),
                                resource.getMetadata().getName(), resource.getMetadata().getNamespace())));
                    if (async) {
                        waitExecutors.add(c);
                    } else {
                        CompletableFuture.allOf(c).join();
                    }
                }
            }
            createCallbacks.forEach(callback -> callback.accept(resource));
        }
        if (!waitExecutors.isEmpty()) {
            CompletableFuture.allOf(waitExecutors.toArray(new CompletableFuture[0])).join();
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
        List<CompletableFuture<Void>> waitExecutors = new LinkedList<>();
        for (T resource : resources) {
            ResourceType<T> type = findResourceType(resource);
            LoggerUtils.logResource("Deleting", resource);
            try {
                if (type == null) {
                    client.getClient().resource(resource).delete();
                    decideDeleteWaitAsync(waitExecutors, async, resource);
                } else {
                    type.delete(resource);
                    decideDeleteWaitAsync(waitExecutors, async, resource);
                }
            } catch (Exception e) {
                if (resource.getMetadata().getNamespace() == null) {
                    LOGGER.error(LoggerUtils.RESOURCE_LOGGER_PATTERN, "Deleting", resource.getKind(),
                        resource.getMetadata().getName(), e);
                } else {
                    LOGGER.error(LoggerUtils.RESOURCE_WITH_NAMESPACE_LOGGER_PATTERN, "Deleting", resource.getKind(),
                        resource.getMetadata().getName(), resource.getMetadata().getNamespace(), e);
                }
            }
            if (!waitExecutors.isEmpty()) {
                CompletableFuture.allOf(waitExecutors.toArray(new CompletableFuture[0])).join();
            }
            deleteCallbacks.forEach(callback -> callback.accept(resource));
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
                client.getClient().resource(resource).update();
            }
        }
    }

    /**
     * Waits for a resource condition to be fulfilled.
     *
     * @param resource  The resource to wait for.
     * @param condition The condition to fulfill.
     * @param <T>       The type of the resource.
     * @return True if the condition is fulfilled, false otherwise.
     */
    public final <T extends HasMetadata> boolean waitResourceCondition(T resource, ResourceCondition<T> condition) {
        assertNotNull(resource);
        assertNotNull(resource.getMetadata());
        assertNotNull(resource.getMetadata().getName());

        ResourceType<T> type = findResourceType(resource);
        boolean[] resourceReady = new boolean[1];

        Wait.until(String.format("Resource condition: %s to be fulfilled for resource %s/%s",
                condition.conditionName(), resource.getKind(), resource.getMetadata().getName()),
            TestFrameConstants.GLOBAL_POLL_INTERVAL_MEDIUM, TestFrameConstants.GLOBAL_TIMEOUT,
            () -> {
                T res = getKubeClient().getClient().resource(resource).get();
                resourceReady[0] = condition.predicate().test(res);
                return resourceReady[0];
            });

        return resourceReady[0];
    }

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
        if (!STORED_RESOURCES.containsKey(getTestContext().getDisplayName())
            || STORED_RESOURCES.get(getTestContext().getDisplayName()).isEmpty()) {
            LOGGER.info("In context {} is everything deleted", getTestContext().getDisplayName());
        } else {
            LOGGER.info("Deleting all resources for {}", getTestContext().getDisplayName());
        }

        // if stack is created for specific test suite or test case
        AtomicInteger numberOfResources = STORED_RESOURCES.get(getTestContext().getDisplayName()) != null ?
            new AtomicInteger(STORED_RESOURCES.get(getTestContext().getDisplayName()).size()) :
            // stack has no elements
            new AtomicInteger(0);
        while (STORED_RESOURCES.containsKey(getTestContext().getDisplayName()) && numberOfResources.get() > 0) {
            Stack<ResourceItem<?>> s = STORED_RESOURCES.get(getTestContext().getDisplayName());
            List<CompletableFuture<Void>> waitExecutors = new LinkedList<>();
            while (!s.isEmpty()) {
                ResourceItem<?> resourceItem = s.pop();

                try {
                    CompletableFuture<Void> c = CompletableFuture.runAsync(() -> {
                        try {
                            resourceItem.throwableRunner().run();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
                    if (async) {
                        waitExecutors.add(c);
                    } else {
                        CompletableFuture.allOf(c).join();
                    }
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
                numberOfResources.decrementAndGet();
                deleteCallbacks.forEach(callback -> {
                    if (resourceItem.resource() != null) {
                        callback.accept(resourceItem.resource());
                    }
                });
            }
            if (!waitExecutors.isEmpty()) {
                CompletableFuture.allOf(waitExecutors.toArray(new CompletableFuture[0])).join();
            }
        }
        STORED_RESOURCES.remove(getTestContext().getDisplayName());
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
        // other no conflicting types
        for (ResourceType<?> type : resourceTypes) {
            if (type.getKind().equals(resource.getKind())) {
                return (ResourceType<T>) type;
            }
        }
        return null;
    }

    private <T extends HasMetadata> boolean isResourceWithReadiness(T resource) {
        return resource instanceof Deployment
            || resource instanceof io.fabric8.kubernetes.api.model.extensions.Deployment
            || resource instanceof ReplicaSet
            || resource instanceof Pod
            || resource instanceof ReplicationController
            || resource instanceof Endpoints
            || resource instanceof Node
            || resource instanceof StatefulSet;
    }

    private void writeResourceAsYaml(HasMetadata resource) {
        File logDir = Paths.get(storeYamlPath)
            .resolve("test-files").resolve(getTestContext().getRequiredTestClass().getName()).toFile();
        if (getTestContext().getTestMethod().isPresent()) {
            logDir = logDir.toPath().resolve(getTestContext().getRequiredTestMethod().getName()).toFile();
        }

        if (!logDir.exists()) {
            if (!logDir.mkdirs()) {
                throw new RuntimeException(
                    String.format("Failed to create root log directories on path: %s", logDir.getAbsolutePath())
                );
            }
        }

        String r = Serialization.asYaml(resource);
        try {
            Files.writeString(logDir.toPath().resolve(
                resource.getKind() + "-" +
                    (resource.getMetadata().getNamespace() == null ? "" :
                        (resource.getMetadata().getNamespace() + "-")) +
                    resource.getMetadata().getName() + ".yaml"), r, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private <T extends HasMetadata> void decideDeleteWaitAsync(List<CompletableFuture<Void>> waitExecutors,
                                                               boolean async, T resource) {
        CompletableFuture<Void> c = CompletableFuture.runAsync(() ->
            assertTrue(waitResourceCondition(resource, ResourceCondition.deletion()),
                String.format("Timed out deleting %s/%s in %s", resource.getKind(),
                    resource.getMetadata().getName(), resource.getMetadata().getNamespace())));
        if (async) {
            waitExecutors.add(c);
        } else {
            CompletableFuture.allOf(c).join();
        }
    }
}
