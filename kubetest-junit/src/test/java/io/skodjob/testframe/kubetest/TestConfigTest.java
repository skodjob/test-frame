/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.kubetest;

import io.skodjob.testframe.kubetest.annotations.CleanupStrategy;
import io.skodjob.testframe.kubetest.annotations.LogCollectionStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for TestConfig record.
 * Tests the data structure and validation of configuration values.
 */
class TestConfigTest {

    @Test
    @DisplayName("Should create TestConfig with minimal configuration")
    void shouldCreateTestConfigWithMinimalConfiguration() {
        // Given
        TestConfig config = new TestConfig(
            new String[]{"test-ns"},
            true,
            CleanupStrategy.AUTOMATIC,
            "",
            false,
            "",
            new String[0],
            new String[0],
            "#",
            76,
            false,
            LogCollectionStrategy.ON_FAILURE,
            "",
            false,
            new String[]{"pods"},
            new String[0],
            false
        );

        // Then
        assertNotNull(config);
        assertArrayEquals(new String[]{"test-ns"}, config.namespaces());
        assertTrue(config.createNamespaces());
        assertEquals(CleanupStrategy.AUTOMATIC, config.cleanup());
        assertEquals("", config.context());
        assertFalse(config.storeYaml());
        assertEquals("", config.yamlStorePath());
        assertEquals(0, config.namespaceLabels().length);
        assertEquals(0, config.namespaceAnnotations().length);
        assertEquals("#", config.visualSeparatorChar());
        assertEquals(76, config.visualSeparatorLength());
        assertFalse(config.collectLogs());
        assertEquals(LogCollectionStrategy.ON_FAILURE, config.logCollectionStrategy());
        assertEquals("", config.logCollectionPath());
        assertFalse(config.collectPreviousLogs());
        assertArrayEquals(new String[]{"pods"}, config.collectNamespacedResources());
        assertEquals(0, config.collectClusterWideResources().length);
        assertFalse(config.collectEvents());
    }

    @Test
    @DisplayName("Should create TestConfig with full configuration")
    void shouldCreateTestConfigWithFullConfiguration() {
        // Given
        String[] namespaces = {"ns1", "ns2", "ns3"};
        String[] namespaceLabels = {"env=test", "team=backend", "version=1.0"};
        String[] namespaceAnnotations = {"description=test namespace", "contact=team@company.com"};
        String[] namespacedResources = {"pods", "services", "configmaps", "secrets", "deployments"};
        String[] clusterWideResources = {"nodes", "persistentvolumes", "storageclasses"};

        TestConfig config = new TestConfig(
            namespaces,
            false,
            CleanupStrategy.MANUAL,
            "production",
            true,
            "/opt/yamls",
            namespaceLabels,
            namespaceAnnotations,
            "=",
            120,
            true,
            LogCollectionStrategy.AFTER_EACH,
            "/var/log/tests",
            true,
            namespacedResources,
            clusterWideResources,
            true
        );

        // Then
        assertArrayEquals(namespaces, config.namespaces());
        assertFalse(config.createNamespaces());
        assertEquals(CleanupStrategy.MANUAL, config.cleanup());
        assertEquals("production", config.context());
        assertTrue(config.storeYaml());
        assertEquals("/opt/yamls", config.yamlStorePath());
        assertArrayEquals(namespaceLabels, config.namespaceLabels());
        assertArrayEquals(namespaceAnnotations, config.namespaceAnnotations());
        assertEquals("=", config.visualSeparatorChar());
        assertEquals(120, config.visualSeparatorLength());
        assertTrue(config.collectLogs());
        assertEquals(LogCollectionStrategy.AFTER_EACH, config.logCollectionStrategy());
        assertEquals("/var/log/tests", config.logCollectionPath());
        assertTrue(config.collectPreviousLogs());
        assertArrayEquals(namespacedResources, config.collectNamespacedResources());
        assertArrayEquals(clusterWideResources, config.collectClusterWideResources());
        assertTrue(config.collectEvents());
    }

    @Test
    @DisplayName("Should handle empty and null arrays correctly")
    void shouldHandleEmptyAndNullArraysCorrectly() {
        // Given
        TestConfig config = new TestConfig(
            new String[0], // Empty namespaces
            true,
            CleanupStrategy.AUTOMATIC,
            "",
            false,
            "",
            new String[0], // Empty labels
            new String[0], // Empty annotations
            "#",
            76,
            false,
            LogCollectionStrategy.NEVER,
            "",
            false,
            new String[0], // Empty namespaced resources
            new String[0], // Empty cluster-wide resources
            false
        );

        // Then
        assertNotNull(config.namespaces());
        assertEquals(0, config.namespaces().length);
        assertNotNull(config.namespaceLabels());
        assertEquals(0, config.namespaceLabels().length);
        assertNotNull(config.namespaceAnnotations());
        assertEquals(0, config.namespaceAnnotations().length);
        assertNotNull(config.collectNamespacedResources());
        assertEquals(0, config.collectNamespacedResources().length);
        assertNotNull(config.collectClusterWideResources());
        assertEquals(0, config.collectClusterWideResources().length);
    }

    @ParameterizedTest
    @ValueSource(strings = {"#", "=", "-", "*", "~"})
    @DisplayName("Should accept various visual separator characters")
    void shouldAcceptVariousVisualSeparatorCharacters(String separatorChar) {
        // Given/When
        TestConfig config = new TestConfig(
            new String[]{"test"},
            true,
            CleanupStrategy.AUTOMATIC,
            "",
            false,
            "",
            new String[0],
            new String[0],
            separatorChar,
            76,
            false,
            LogCollectionStrategy.ON_FAILURE,
            "",
            false,
            new String[0],
            new String[0],
            false
        );

        // Then
        assertEquals(separatorChar, config.visualSeparatorChar());
    }

    @ParameterizedTest
    @ValueSource(ints = {50, 76, 100, 120, 200})
    @DisplayName("Should accept various visual separator lengths")
    void shouldAcceptVariousVisualSeparatorLengths(int length) {
        // Given/When
        TestConfig config = new TestConfig(
            new String[]{"test"},
            true,
            CleanupStrategy.AUTOMATIC,
            "",
            false,
            "",
            new String[0],
            new String[0],
            "#",
            length,
            false,
            LogCollectionStrategy.ON_FAILURE,
            "",
            false,
            new String[0],
            new String[0],
            false
        );

        // Then
        assertEquals(length, config.visualSeparatorLength());
    }

    @Test
    @DisplayName("Should support all cleanup strategies")
    void shouldSupportAllCleanupStrategies() {
        for (CleanupStrategy strategy : CleanupStrategy.values()) {
            // Given/When
            TestConfig config = new TestConfig(
                new String[]{"test"},
                true,
                strategy,
                "",
                false,
                "",
                new String[0],
                new String[0],
                "#",
                76,
                false,
                LogCollectionStrategy.ON_FAILURE,
                "",
                false,
                new String[0],
                new String[0],
                false
            );

            // Then
            assertEquals(strategy, config.cleanup());
        }
    }

    @Test
    @DisplayName("Should support all log collection strategies")
    void shouldSupportAllLogCollectionStrategies() {
        for (LogCollectionStrategy strategy : LogCollectionStrategy.values()) {
            // Given/When
            TestConfig config = new TestConfig(
                new String[]{"test"},
                true,
                CleanupStrategy.AUTOMATIC,
                "",
                false,
                "",
                new String[0],
                new String[0],
                "#",
                76,
                false,
                strategy,
                "",
                false,
                new String[0],
                new String[0],
                false
            );

            // Then
            assertEquals(strategy, config.logCollectionStrategy());
        }
    }

    @Test
    @DisplayName("Should validate boolean flag combinations")
    void shouldValidateBooleanFlagCombinations() {
        // Test all combinations of boolean flags
        boolean[] values = {true, false};

        for (boolean createNamespaces : values) {
            for (boolean storeYaml : values) {
                for (boolean collectLogs : values) {
                    for (boolean collectPreviousLogs : values) {
                        for (boolean collectEvents : values) {
                            // Given/When
                            TestConfig config = new TestConfig(
                                new String[]{"test"},
                                createNamespaces,
                                CleanupStrategy.AUTOMATIC,
                                "",
                                storeYaml,
                                "",
                                new String[0],
                                new String[0],
                                "#",
                                76,
                                collectLogs,
                                LogCollectionStrategy.ON_FAILURE,
                                "",
                                collectPreviousLogs,
                                new String[0],
                                new String[0],
                                collectEvents
                            );

                            // Then
                            assertEquals(createNamespaces, config.createNamespaces());
                            assertEquals(storeYaml, config.storeYaml());
                            assertEquals(collectLogs, config.collectLogs());
                            assertEquals(collectPreviousLogs, config.collectPreviousLogs());
                            assertEquals(collectEvents, config.collectEvents());
                        }
                    }
                }
            }
        }
    }
}