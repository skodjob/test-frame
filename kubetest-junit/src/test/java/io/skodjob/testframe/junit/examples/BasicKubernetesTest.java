/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.junit.examples;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.skodjob.testframe.annotations.RequiresKubernetes;
import io.skodjob.testframe.clients.KubeClient;
import io.skodjob.testframe.clients.cmdClient.KubeCmdClient;
import io.skodjob.testframe.junit.annotations.CleanupStrategy;
import io.skodjob.testframe.junit.annotations.InjectCmdKubeClient;
import io.skodjob.testframe.junit.annotations.InjectKubeClient;
import io.skodjob.testframe.junit.annotations.InjectNamespace;
import io.skodjob.testframe.junit.annotations.InjectResourceManager;
import io.skodjob.testframe.junit.annotations.KubernetesTest;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Example test demonstrating basic usage of the Kubernetes test framework extension.
 * This test shows how to:
 * - Use @KubernetesTest annotation
 * - Inject KubeClient and KubeResourceManager
 * - Inject namespace information
 * - Create and manage resources
 */
@RequiresKubernetes
@KubernetesTest(
    namespace = "basic-test",
    cleanup = CleanupStrategy.AFTER_EACH,
    createNamespace = true,
    namespaceLabels = {"test-type=basic", "framework=test-frame"},
    namespaceAnnotations = {"description=Basic test example"}
)
class BasicKubernetesTest {

    @InjectKubeClient
    KubeClient client;

    @InjectResourceManager
    KubeResourceManager resourceManager;

    @InjectCmdKubeClient
    KubeCmdClient<?> kubeCmdClient;

    @InjectNamespace
    String namespaceName;

    @InjectNamespace
    Namespace namespace;

    @Test
    void testBasicFunctionality() {
        // Verify injections work
        assertNotNull(client, "KubeClient should be injected");
        assertNotNull(resourceManager, "KubeResourceManager should be injected");
        assertEquals("basic-test", namespaceName, "Namespace name should be injected");
        assertNotNull(namespace, "Namespace object should be injected");

        // Verify namespace labels and annotations
        assertEquals("basic", namespace.getMetadata().getLabels().get("test-type"));
        assertEquals("test-frame", namespace.getMetadata().getLabels().get("framework"));
        assertEquals("Basic test example", namespace.getMetadata().getAnnotations().get("description"));
    }

    @Test
    void testResourceCreation(@InjectKubeClient KubeClient paramClient,
                              @InjectResourceManager KubeResourceManager paramResourceManager) {
        // Demonstrate parameter injection
        assertNotNull(paramClient, "Parameter injection should work for KubeClient");
        assertNotNull(paramResourceManager, "Parameter injection should work for KubeResourceManager");

        // Create a ConfigMap
        ConfigMap configMap = new ConfigMapBuilder()
            .withNewMetadata()
            .withName("test-config")
            .withNamespace(namespaceName)
            .endMetadata()
            .addToData("key1", "value1")
            .addToData("key2", "value2")
            .build();

        // Create and verify the resource
        resourceManager.createResourceWithWait(configMap);

        kubeCmdClient.exec("get", "ns");

        ConfigMap createdConfigMap = client.getClient().configMaps()
            .inNamespace(namespaceName)
            .withName("test-config")
            .get();

        assertNotNull(createdConfigMap, "ConfigMap should be created");
        assertEquals("value1", createdConfigMap.getData().get("key1"));
        assertEquals("value2", createdConfigMap.getData().get("key2"));
    }

    @Test
    void testPodCreation() {
        // Create a simple pod
        Pod pod = new PodBuilder()
            .withNewMetadata()
            .withName("test-pod")
            .withNamespace(namespaceName)
            .endMetadata()
            .withNewSpec()
            .addNewContainer()
            .withName("test-container")
            .withImage("quay.io/prometheus/busybox")
            .withCommand("sleep", "300")
            .endContainer()
            .withRestartPolicy("Never")
            .endSpec()
            .build();

        resourceManager.createResourceWithWait(pod);

        Pod createdPod = client.getClient().pods()
            .inNamespace(namespaceName)
            .withName("test-pod")
            .get();

        assertNotNull(createdPod, "Pod should be created");
        assertEquals("test-pod", createdPod.getMetadata().getName());
        assertEquals(namespaceName, createdPod.getMetadata().getNamespace());
    }
}