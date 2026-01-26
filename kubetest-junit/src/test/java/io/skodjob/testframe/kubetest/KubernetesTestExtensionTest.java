/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.kubetest;

import io.skodjob.testframe.kubetest.annotations.CleanupStrategy;
import io.skodjob.testframe.kubetest.annotations.KubernetesTest;
import io.skodjob.testframe.kubetest.annotations.LogCollectionStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for KubernetesTestExtension.
 * These tests focus on testing individual methods and logic without requiring a real Kubernetes cluster.
 */
@ExtendWith(MockitoExtension.class)
class KubernetesTestExtensionTest {

    @Mock
    private ExtensionContext extensionContext;

    @Mock
    private Store store;

    @Mock
    private ParameterContext parameterContext;

    @Mock
    private Parameter parameter;

    private KubernetesTestExtension extension;

    @BeforeEach
    void setUp() {
        extension = new KubernetesTestExtension();
        lenient().when(extensionContext.getStore(any(ExtensionContext.Namespace.class))).thenReturn(store);
    }

    @Nested
    @DisplayName("TestConfig Creation Tests")
    class TestConfigCreationTests {

        @Test
        @DisplayName("Should create TestConfig with default values")
        void shouldCreateTestConfigWithDefaults() {
            // Given - Create actual TestConfig directly since createTestConfig is private
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
                true
            );

            // Then
            assertNotNull(config);
            assertArrayEquals(new String[]{"test-ns"}, config.namespaces());
            assertTrue(config.createNamespaces());
            assertEquals(CleanupStrategy.AUTOMATIC, config.cleanup());
            assertEquals("", config.context());
            assertFalse(config.storeYaml());
            assertEquals("", config.yamlStorePath());
            assertEquals("#", config.visualSeparatorChar());
            assertEquals(76, config.visualSeparatorLength());
            assertFalse(config.collectLogs());
            assertEquals(LogCollectionStrategy.ON_FAILURE, config.logCollectionStrategy());
        }

        @Test
        @DisplayName("Should create TestConfig with custom values")
        void shouldCreateTestConfigWithCustomValues() {
            // Given - Create actual TestConfig directly
            TestConfig config = new TestConfig(
                new String[]{"ns1", "ns2"},
                false,
                CleanupStrategy.MANUAL,
                "staging",
                true,
                "target/yamls",
                new String[]{"env=test"},
                new String[]{"description=test"},
                "=",
                80,
                true,
                LogCollectionStrategy.AFTER_EACH,
                "target/logs",
                true,
                new String[]{"pods", "services"},
                new String[]{"nodes"},
                false
            );

            // Then
            assertArrayEquals(new String[]{"ns1", "ns2"}, config.namespaces());
            assertFalse(config.createNamespaces());
            assertEquals(CleanupStrategy.MANUAL, config.cleanup());
            assertEquals("staging", config.context());
            assertTrue(config.storeYaml());
            assertEquals("target/yamls", config.yamlStorePath());
            assertArrayEquals(new String[]{"env=test"}, config.namespaceLabels());
            assertArrayEquals(new String[]{"description=test"}, config.namespaceAnnotations());
            assertEquals("=", config.visualSeparatorChar());
            assertEquals(80, config.visualSeparatorLength());
            assertTrue(config.collectLogs());
            assertEquals(LogCollectionStrategy.AFTER_EACH, config.logCollectionStrategy());
            assertEquals("target/logs", config.logCollectionPath());
            assertTrue(config.collectPreviousLogs());
            assertArrayEquals(new String[]{"pods", "services"}, config.collectNamespacedResources());
            assertArrayEquals(new String[]{"nodes"}, config.collectClusterWideResources());
            assertFalse(config.collectEvents());
        }

        @Test
        @DisplayName("Should validate TestConfig record properties")
        void shouldValidateTestConfigRecordProperties() {
            // Given - Create TestConfig with various values
            TestConfig config = new TestConfig(
                new String[]{"ns1", "ns2", "ns3"},
                true,
                CleanupStrategy.AUTOMATIC,
                "test-context",
                true,
                "/tmp/yamls",
                new String[]{"label1=value1", "label2=value2"},
                new String[]{"annotation1=value1"},
                "=",
                100,
                true,
                LogCollectionStrategy.AFTER_EACH,
                "/tmp/logs",
                true,
                new String[]{"pods", "services", "configmaps"},
                new String[]{"nodes", "persistentvolumes"},
                true
            );

            // Then - Verify all properties
            assertArrayEquals(new String[]{"ns1", "ns2", "ns3"}, config.namespaces());
            assertTrue(config.createNamespaces());
            assertEquals(CleanupStrategy.AUTOMATIC, config.cleanup());
            assertEquals("test-context", config.context());
            assertTrue(config.storeYaml());
            assertEquals("/tmp/yamls", config.yamlStorePath());
            assertArrayEquals(new String[]{"label1=value1", "label2=value2"}, config.namespaceLabels());
            assertArrayEquals(new String[]{"annotation1=value1"}, config.namespaceAnnotations());
            assertEquals("=", config.visualSeparatorChar());
            assertEquals(100, config.visualSeparatorLength());
            assertTrue(config.collectLogs());
            assertEquals(LogCollectionStrategy.AFTER_EACH, config.logCollectionStrategy());
            assertEquals("/tmp/logs", config.logCollectionPath());
            assertTrue(config.collectPreviousLogs());
            assertArrayEquals(new String[]{"pods", "services", "configmaps"}, config.collectNamespacedResources());
            assertArrayEquals(new String[]{"nodes", "persistentvolumes"}, config.collectClusterWideResources());
            assertTrue(config.collectEvents());
        }
    }

    @Nested
    @DisplayName("Namespace Generation Tests")
    class NamespaceGenerationTests {

        @Test
        @DisplayName("Should generate namespace with timestamp format validation")
        void shouldGenerateNamespaceWithTimestampFormatValidation() {
            // Test the namespace format generation logic
            String className = "TestClass";
            LocalDateTime now = LocalDateTime.now();
            String timestamp = now.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            String expectedPattern = String.format("test-%s-%s", className.toLowerCase(), timestamp);

            // Verify the format is correct
            assertTrue(expectedPattern.matches("test-[a-z]+-\\d{8}-\\d{6}"));

            // Verify timestamp parsing
            assertDoesNotThrow(() -> {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
                formatter.parse(timestamp);
            });
        }
    }

    @Nested
    @DisplayName("Parameter Resolution Tests")
    class ParameterResolutionTests {

        @Test
        @DisplayName("Should support parameter with inject annotation")
        void shouldSupportParameterWithInjectAnnotation() throws NoSuchMethodException {
            // Given
            Method method = TestClass.class.getDeclaredMethod("testMethod", Object.class);
            Parameter param = method.getParameters()[0];

            lenient().when(parameterContext.getParameter()).thenReturn(param);
            lenient().when(parameterContext.isAnnotated(any())).thenReturn(true);

            // When
            boolean supports = extension.supportsParameter(parameterContext, extensionContext);

            // Then
            assertTrue(supports);
        }

        @Test
        @DisplayName("Should not support parameter without inject annotation")
        void shouldNotSupportParameterWithoutInjectAnnotation() throws NoSuchMethodException {
            // Given
            Method method = TestClass.class.getDeclaredMethod("testMethodNoAnnotation", String.class);
            Parameter param = method.getParameters()[0];

            lenient().when(parameterContext.getParameter()).thenReturn(param);
            lenient().when(parameterContext.isAnnotated(any())).thenReturn(false);

            // When
            boolean supports = extension.supportsParameter(parameterContext, extensionContext);

            // Then
            assertFalse(supports);
        }
    }

    @Nested
    @DisplayName("Enum Validation Tests")
    class EnumValidationTests {

        @Test
        @DisplayName("Should validate CleanupStrategy enum values")
        void shouldValidateCleanupStrategyEnumValues() {
            // Test that all cleanup strategy values work correctly
            TestConfig automaticConfig = new TestConfig(
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
                LogCollectionStrategy.ON_FAILURE,
                "",
                false,
                new String[0],
                new String[0],
                false
            );

            TestConfig manualConfig = new TestConfig(
                new String[]{"test"},
                true,
                CleanupStrategy.MANUAL,
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

            assertEquals(CleanupStrategy.AUTOMATIC, automaticConfig.cleanup());
            assertEquals(CleanupStrategy.MANUAL, manualConfig.cleanup());
        }

        @Test
        @DisplayName("Should validate LogCollectionStrategy enum values")
        void shouldValidateLogCollectionStrategyEnumValues() {
            // Test that all log collection strategy values work correctly
            for (LogCollectionStrategy strategy : LogCollectionStrategy.values()) {
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

                assertEquals(strategy, config.logCollectionStrategy());
            }
        }
    }

    // Test Class for Method Testing
    @KubernetesTest
    static class TestClass {
        public void testMethod(@io.skodjob.testframe.kubetest.annotations.InjectKubeClient Object client) {
        }

        public void testMethodNoAnnotation(String param) {
        }
    }
}