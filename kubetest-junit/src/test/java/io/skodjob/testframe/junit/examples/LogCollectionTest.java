/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.junit.examples;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.skodjob.testframe.annotations.RequiresKubernetes;
import io.skodjob.testframe.clients.KubeClient;
import io.skodjob.testframe.junit.annotations.CleanupStrategy;
import io.skodjob.testframe.junit.annotations.InjectKubeClient;
import io.skodjob.testframe.junit.annotations.InjectResourceManager;
import io.skodjob.testframe.junit.annotations.KubernetesTest;
import io.skodjob.testframe.junit.annotations.LogCollectionStrategy;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Example test demonstrating integrated log collection with @KubernetesTest.
 * Shows how to configure automatic log collection when tests fail or complete.
 */
@RequiresKubernetes
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@KubernetesTest(
    namespace = "log-collection-test",
    cleanup = CleanupStrategy.AFTER_ALL,
    createNamespace = true,

    // ===== Log Collection Configuration =====
    collectLogs = true,
    logCollectionStrategy = LogCollectionStrategy.AFTER_EACH,
    logCollectionPath = "target/test-logs/log-collection-test",
    collectPreviousLogs = true,
    collectNamespacedResources = {"pods", "services", "configmaps", "secrets", "deployments"},
    collectClusterWideResources = {"nodes"},
    collectEvents = true
)
class LogCollectionTest {

    @InjectKubeClient
    KubeClient client;

    @InjectResourceManager
    KubeResourceManager resourceManager;

    @Test
    @Order(1)
    void testSuccessfulOperation() {
        // Create a simple ConfigMap - this should succeed
        ConfigMap configMap = new ConfigMapBuilder()
            .withNewMetadata()
            .withName("test-config")
            .withNamespace("log-collection-test")
            .endMetadata()
            .addToData("key", "value")
            .build();

        resourceManager.createResourceWithWait(configMap);

        // Verify it was created
        ConfigMap retrieved = client.getClient().configMaps()
            .inNamespace("log-collection-test")
            .withName("test-config")
            .get();

        assertNotNull(retrieved);
    }

    @Test
    @Order(2)
    void testCheckLogs() {
        assertTrue(Files.exists(Paths.get(System.getProperty("user.dir"),
            "/target/test-logs/log-collection-test/log-collection-test/configmaps")));
        assertTrue(Files.exists(Paths.get(System.getProperty("user.dir"),
            "target/test-logs/log-collection-test/log-collection-test/secrets")));
    }
}