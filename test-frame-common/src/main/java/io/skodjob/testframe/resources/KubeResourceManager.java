/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.resources;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.skodjob.testframe.LoggerUtils;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Manages Kubernetes resources for testing purposes.
 */
public class KubeResourceManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(KubeResourceManager.class);
    private static KubeResourceManager instance;
    private static KubeClient client;
    private static KubeCmdClient kubeCmdClient;
    private static ThreadLocal<ExtensionContext> testContext = new ThreadLocal<>();
    private static final Map<String, Stack<ResourceItem>> STORED_RESOURCES = new LinkedHashMap<>();
    private ResourceType<?>[] resourceTypes;

    /**
     * Retrieves the singleton instance of KubeResourceManager.
     * @return The singleton instance of KubeResourceManager.
     */
    public static synchronized KubeResourceManager getInstance() {
        if (instance == null) {
            instance = new KubeResourceManager();
            instance.resourceTypes = new ResourceType[]{};
            client = new KubeClient();
            if (TestFrameEnv.CLIENT_TYPE.equals(TestFrameConstants.KUBERNETES_CLIENT)) {
                kubeCmdClient = new Kubectl();
            } else {
                kubeCmdClient = new Oc(client.getKubeconfigPath());
            }
        }
        return instance;
    }

    /**
     * Retrieves the Kubernetes client.
     * @return The Kubernetes client.
     */
    public static KubeClient getKubeClient() {
        return client;
    }

    /**
     * Retrieves the Kubernetes command-line client.
     * @return The Kubernetes command-line client.
     */
    public static KubeCmdClient getKubeCmdClient() {
        return kubeCmdClient;
    }

    /**
     * Sets the test context.
     * @param context The extension context.
     */
    public static void setTestContext(ExtensionContext context) {
        testContext.set(context);
    }

    /**
     * Retrieves the test context.
     * @return The extension context.
     */
    public static ExtensionContext getTestContext() {
        return testContext.get();
    }

    /**
     * Sets the resource types.
     * @param types The resource types implementing {@link ResourceType}
     */
    public final void setResourceTypes(ResourceType... types) {
        this.resourceTypes = types;
    }

    /**
     * Pushes a resource item to the stack.
     * @param item The resource item to push.
     */
    public final void pushToStack(ResourceItem item) {
        synchronized (this) {
            STORED_RESOURCES.computeIfAbsent(getTestContext().getDisplayName(), k -> new Stack<>());
            STORED_RESOURCES.get(getTestContext().getDisplayName()).push(item);
        }
    }

    /**
     * Pushes a resource to the stack.
     * @param resource The resource to push.
     * @param <T> The type of the resource.
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
     * Creates resources without waiting for readiness.
     * @param resources The resources to create.
     * @param <T> The type of the resources.
     */
    @SafeVarargs
    public final <T extends HasMetadata> void createResourceWithoutWait(T... resources) {
        createResource(false, resources);
    }

    /**
     * Creates resources and waits for readiness.
     * @param resources The resources to create.
     * @param <T> The type of the resources.
     */
    @SafeVarargs
    public final <T extends HasMetadata> void createResourceWithWait(T... resources) {
        createResource(true, resources);
    }

    /**
     * Creates resources with or without waiting for readiness.
     * @param waitReady Flag indicating whether to wait for readiness.
     * @param resources The resources to create.
     * @param <T> The type of the resources.
     */
    @SafeVarargs
    private <T extends HasMetadata> void createResource(boolean waitReady, T... resources) {
        for (T resource : resources) {
            ResourceType<T> type = findResourceType(resource);
            pushToStack(resource);

            if (resource.getMetadata().getNamespace() == null) {
                LOGGER.info(LoggerUtils.RESOURCE_LOGGER_PATTERN,
                        "Creating", resource.getKind(), resource.getMetadata().getName());
            } else {
                LOGGER.info(LoggerUtils.RESOURCE_WITH_NAMESPACE_LOGGER_PATTERN,
                        "Creating", resource.getKind(), resource.getMetadata().getName(),
                        resource.getMetadata().getNamespace());
            }

            if (type == null) {
                // Generic create for any resource
                client.getClient().resource(resource).create();
                if (waitReady) {
                    assertTrue(waitResourceCondition(resource, new ResourceCondition<>(p -> {
                        try {
                            return client.getClient().resource(resource).isReady();
                        } catch (Exception ex) {
                            return client.getClient().resource(resource) != null;
                        }
                    }, "ready")),
                            String.format("Timed out waiting for %s/%s in %s to be ready", resource.getKind(),
                                    resource.getMetadata().getName(), resource.getMetadata().getNamespace()));
                }
            } else {
                // Create for typed resource implementing ResourceType
                type.create(resource);
                if (waitReady) {
                    assertTrue(waitResourceCondition(resource, ResourceCondition.readiness(type)),
                            String.format("Timed out waiting for %s/%s in %s to be ready", resource.getKind(),
                                    resource.getMetadata().getName(), resource.getMetadata().getNamespace()));
                }
            }
        }
    }

    /**
     * Deletes resources.
     * @param resources The resources to delete.
     * @param <T> The type of the resources.
     */
    @SafeVarargs
    public final <T extends HasMetadata> void deleteResource(T... resources) {
        for (T resource : resources) {
            ResourceType<T> type = findResourceType(resource);
            if (resource.getMetadata().getNamespace() == null) {
                LOGGER.info(LoggerUtils.RESOURCE_LOGGER_PATTERN,
                        "Deleting", resource.getKind(), resource.getMetadata().getName());
            } else {
                LOGGER.info(LoggerUtils.RESOURCE_WITH_NAMESPACE_LOGGER_PATTERN,
                        "Deleting", resource.getKind(), resource.getMetadata().getName(),
                        resource.getMetadata().getNamespace());
            }
            try {
                if (type == null) {
                    client.getClient().resource(resource).delete();
                    assertTrue(waitResourceCondition(resource, ResourceCondition.deletion()),
                            String.format("Timed out deleting %s/%s in %s", resource.getKind(),
                                    resource.getMetadata().getName(), resource.getMetadata().getNamespace()));
                } else {
                    type.delete(resource.getMetadata().getName());
                    assertTrue(waitResourceCondition(resource, ResourceCondition.deletion()),
                            String.format("Timed out deleting %s/%s in %s", resource.getKind(),
                                    resource.getMetadata().getName(), resource.getMetadata().getNamespace()));
                }
            } catch (Exception e) {
                if (resource.getMetadata().getNamespace() == null) {
                    LOGGER.error(LoggerUtils.RESOURCE_LOGGER_PATTERN, "Deleting", resource.getKind(),
                            resource.getMetadata().getName(), e);
                } else {
                    LOGGER.error(LoggerUtils.RESOURCE_WITH_NAMESPACE_LOGGER_PATTERN, "Deleting",resource.getKind(),
                            resource.getMetadata().getName(), resource.getMetadata().getNamespace(), e);
                }
            }
        }
    }

    /**
     * Updates resources.
     * @param resources The resources to update.
     * @param <T> The type of the resources.
     */
    @SafeVarargs
    public final <T extends HasMetadata> void updateResource(T... resources) {
        for (T resource : resources) {
            if (resource.getMetadata().getNamespace() == null) {
                LOGGER.info(LoggerUtils.RESOURCE_LOGGER_PATTERN,
                        "Updating", resource.getKind(), resource.getMetadata().getName());
            } else {
                LOGGER.info(LoggerUtils.RESOURCE_WITH_NAMESPACE_LOGGER_PATTERN,
                        "Updating", resource.getKind(), resource.getMetadata().getName(),
                        resource.getMetadata().getNamespace());
            }
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
     * @param resource The resource to wait for.
     * @param condition The condition to fulfill.
     * @param <T> The type of the resource.
     * @return True if the condition is fulfilled, false otherwise.
     */
    public final <T extends HasMetadata> boolean waitResourceCondition(T resource, ResourceCondition<T> condition) {
        assertNotNull(resource);
        assertNotNull(resource.getMetadata());
        assertNotNull(resource.getMetadata().getName());

        ResourceType<T> type = findResourceType(resource);
        boolean[] resourceReady = new boolean[1];

        Wait.until(String.format("Resource condition: %s to be fulfilled for resource %s/%s",
                        condition.getConditionName(), resource.getKind(), resource.getMetadata().getName()),
                TestFrameConstants.GLOBAL_POLL_INTERVAL_MEDIUM, TestFrameConstants.GLOBAL_TIMEOUT,
                () -> {
                    T res = getKubeClient().getClient().resource(resource).get();
                    resourceReady[0] = condition.getPredicate().test(res);
                    return resourceReady[0];
                },
                () -> {
                    T res = getKubeClient().getClient().resource(resource).get();
                    if (type == null) {
                        client.getClient().resource(resource).delete();
                    } else {
                        type.delete(res.getMetadata().getName());
                    }
                });

        return resourceReady[0];
    }

    /**
     * Deletes all stored resources.
     */
    public void deleteResources() {
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
            Stack<ResourceItem> s = STORED_RESOURCES.get(getTestContext().getDisplayName());

            while (!s.isEmpty()) {
                ResourceItem resourceItem = s.pop();

                try {
                    resourceItem.getThrowableRunner().run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                numberOfResources.decrementAndGet();
            }
        }
        STORED_RESOURCES.remove(getTestContext().getDisplayName());
        LoggerUtils.logSeparator();
    }

    /**
     * Return ResourceType implementation if it is specified in resourceTypes based on kind
     * @param resource HasMetadata resource to find
     * @return {@link ResourceType}
     * @param <T> The type of the resource.
     */
    private <T extends HasMetadata> ResourceType<T> findResourceType(T resource) {
        // other no conflicting types
        for (ResourceType<?> type : resourceTypes) {
            if (type.getKind().equals(resource.getKind())) {
                return (ResourceType<T>) type;
            }
        }
        return null;
    }
}
