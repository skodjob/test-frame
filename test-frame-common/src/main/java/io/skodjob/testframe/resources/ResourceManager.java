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

public class ResourceManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceManager.class);

    private static ResourceManager instance;
    private static KubeClient client;
    private static KubeCmdClient kubeCmdClient;

    private static ThreadLocal<ExtensionContext> testContext = new ThreadLocal<>();

    private static final Map<String, Stack<ResourceItem>> STORED_RESOURCES = new LinkedHashMap<>();

    public static synchronized ResourceManager getInstance() {
        if (instance == null) {
            instance = new ResourceManager();
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

    public static KubeClient getKubeClient() {
        return client;
    }

    public static KubeCmdClient getKubeCmdClient() {
        return kubeCmdClient;
    }

    private ResourceType<?>[] resourceTypes;

    public static void setTestContext(ExtensionContext context) {
        testContext.set(context);
    }

    public static ExtensionContext getTestContext() {
        return testContext.get();
    }

    public final void setResourceTypes(ResourceType... types) {
        this.resourceTypes = types;
    }

    public final void pushToStack(ResourceItem item) {
        synchronized (this) {
            STORED_RESOURCES.computeIfAbsent(getTestContext().getDisplayName(), k -> new Stack<>());
            STORED_RESOURCES.get(getTestContext().getDisplayName()).push(item);
        }
    }

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

    @SafeVarargs
    public final <T extends HasMetadata> void createResourceWithoutWait(T... resources) {
        createResource(false, resources);
    }

    @SafeVarargs
    public final <T extends HasMetadata> void createResourceWithWait(T... resources) {
        createResource(true, resources);
    }

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

    public final <T extends HasMetadata> boolean waitResourceCondition(T resource, ResourceCondition<T> condition) {
        assertNotNull(resource);
        assertNotNull(resource.getMetadata());
        assertNotNull(resource.getMetadata().getName());

        ResourceType<T> type = findResourceType(resource);
        boolean[] resourceReady = new boolean[1];

        Wait.until(String.format("Resource condition: %s  to be fulfilled for resource %s/%s",
                        condition.getConditionName(), resource.getKind(), resource.getMetadata().getName()),
                TestFrameConstants.GLOBAL_POLL_INTERVAL_MEDIUM, TestFrameConstants.GLOBAL_TIMEOUT,
                () -> {
                    T res = getKubeClient().getClient().resource(resource).get();
                    resourceReady[0] = condition.getPredicate().test(res);
                    if (!resourceReady[0]) {
                        if (type == null) {
                            client.getClient().resource(resource).delete();
                        } else {
                            type.delete(res.getMetadata().getName());
                        }
                    }
                    return resourceReady[0];
                });

        return resourceReady[0];
    }

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
