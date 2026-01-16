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
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ConfigurationManager.
 * Tests configuration creation and management logic.
 */
@ExtendWith(MockitoExtension.class)
class ConfigurationManagerTest {

    @Mock
    private ContextStoreHelper contextStoreHelper;

    @Mock
    private ExtensionContext extensionContext;

    @Mock
    private Store store;

    // Use a real test class instead of mocking Class<?>
    private Class<?> testClass = ConfigurationManagerTest.class;

    private ConfigurationManager configurationManager;

    @BeforeEach
    void setUp() {
        configurationManager = new ConfigurationManager(contextStoreHelper);
        lenient().when(extensionContext.getStore(any(ExtensionContext.Namespace.class))).thenReturn(store);
        lenient().when(extensionContext.getRequiredTestClass()).thenReturn((Class) testClass);
    }

    @Nested
    @DisplayName("Annotation Retrieval Tests")
    class AnnotationRetrievalTests {

        @Test
        @DisplayName("Should get KubernetesTest annotation from test class")
        void shouldGetKubernetesTestAnnotationFromTestClass() {
            // When - ConfigurationManagerTest doesn't have @KubernetesTest annotation
            KubernetesTest result = configurationManager.getKubernetesTestAnnotation(extensionContext);

            // Then - should return null since the real test class has no annotation
            assertNull(result);
        }

        @Test
        @DisplayName("Should return null when annotation is not present")
        void shouldReturnNullWhenAnnotationIsNotPresent() {
            // When - using real class without @KubernetesTest annotation
            KubernetesTest result = configurationManager.getKubernetesTestAnnotation(extensionContext);

            // Then
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("TestConfig Creation Tests")
    class TestConfigCreationTests {

        @Test
        @DisplayName("Should create TestConfig with provided namespaces")
        void shouldCreateTestConfigWithProvidedNamespaces() {
            // Given
            KubernetesTest annotation = createMockKubernetesTestAnnotation();
            lenient().when(annotation.namespaces()).thenReturn(new String[]{"ns1", "ns2"});

            // When
            TestConfig config = configurationManager.createTestConfig(extensionContext, annotation);

            // Then
            assertEquals(List.of("ns1", "ns2"), config.namespaces());
        }

        @Test
        @DisplayName("Should generate namespace when none provided")
        void shouldGenerateNamespaceWhenNoneProvided() {
            // Given
            KubernetesTest annotation = createMockKubernetesTestAnnotation();
            lenient().when(annotation.namespaces()).thenReturn(new String[0]);

            // When
            TestConfig config = configurationManager.createTestConfig(extensionContext, annotation);

            // Then
            assertEquals(1, config.namespaces().size());
            assertTrue(config.namespaces().get(0).startsWith("test-configurationmanagertest-"));
            assertTrue(config.namespaces().get(0).length() > 30); // Should have timestamp suffix
        }

        @Test
        @DisplayName("Should create TestConfig with all annotation properties")
        void shouldCreateTestConfigWithAllAnnotationProperties() {
            // Given
            KubernetesTest annotation = createMockKubernetesTestAnnotation();
            when(annotation.namespaces()).thenReturn(new String[]{"test-ns"});
            when(annotation.createNamespaces()).thenReturn(true);
            when(annotation.cleanup()).thenReturn(CleanupStrategy.MANUAL);
            when(annotation.context()).thenReturn("test-context");
            when(annotation.storeYaml()).thenReturn(true);
            when(annotation.yamlStorePath()).thenReturn("/test/path");
            when(annotation.namespaceLabels()).thenReturn(new String[]{"label=value"});
            when(annotation.namespaceAnnotations()).thenReturn(new String[]{"annotation=value"});
            when(annotation.visualSeparatorChar()).thenReturn("=");
            when(annotation.visualSeparatorLength()).thenReturn(100);
            when(annotation.collectLogs()).thenReturn(true);
            when(annotation.logCollectionStrategy()).thenReturn(LogCollectionStrategy.AFTER_EACH);
            when(annotation.logCollectionPath()).thenReturn("/logs");
            when(annotation.collectPreviousLogs()).thenReturn(true);
            when(annotation.collectNamespacedResources()).thenReturn(new String[]{"pods", "services"});
            when(annotation.collectClusterWideResources()).thenReturn(new String[]{"nodes"});
            when(annotation.contextMappings()).thenReturn(new KubernetesTest.ContextMapping[0]);

            // When
            TestConfig config = configurationManager.createTestConfig(extensionContext, annotation);

            // Then
            assertEquals(List.of("test-ns"), config.namespaces());
            assertTrue(config.createNamespaces());
            assertEquals(CleanupStrategy.MANUAL, config.cleanup());
            assertEquals("test-context", config.context());
            assertTrue(config.storeYaml());
            assertEquals("/test/path", config.yamlStorePath());
            assertEquals(List.of("label=value"), config.namespaceLabels());
            assertEquals(List.of("annotation=value"), config.namespaceAnnotations());
            assertEquals("=", config.visualSeparatorChar());
            assertEquals(100, config.visualSeparatorLength());
            assertTrue(config.collectLogs());
            assertEquals(LogCollectionStrategy.AFTER_EACH, config.logCollectionStrategy());
            assertEquals("/logs", config.logCollectionPath());
            assertTrue(config.collectPreviousLogs());
            assertEquals(List.of("pods", "services"), config.collectNamespacedResources());
            assertEquals(List.of("nodes"), config.collectClusterWideResources());
        }

        @Test
        @DisplayName("Should convert context mappings from annotation")
        void shouldConvertContextMappingsFromAnnotation() {
            // Given
            KubernetesTest annotation = createMockKubernetesTestAnnotation();
            KubernetesTest.ContextMapping contextMapping = createMockContextMappingAnnotation();
            when(annotation.contextMappings()).thenReturn(new KubernetesTest.ContextMapping[]{contextMapping});
            when(annotation.namespaces()).thenReturn(new String[]{"test-ns"});

            when(contextMapping.context()).thenReturn("staging");
            when(contextMapping.namespaces()).thenReturn(new String[]{"stg-ns1", "stg-ns2"});
            when(contextMapping.createNamespaces()).thenReturn(true);
            when(contextMapping.cleanup()).thenReturn(CleanupStrategy.AUTOMATIC);
            when(contextMapping.namespaceLabels()).thenReturn(new String[]{"env=staging"});
            when(contextMapping.namespaceAnnotations()).thenReturn(new String[]{"deploy=auto"});

            // When
            TestConfig config = configurationManager.createTestConfig(extensionContext, annotation);

            // Then
            assertEquals(1, config.contextMappings().size());
            TestConfig.ContextMappingConfig mapping = config.contextMappings().get(0);
            assertEquals("staging", mapping.context());
            assertEquals(List.of("stg-ns1", "stg-ns2"), mapping.namespaces());
            assertTrue(mapping.createNamespaces());
            assertEquals(CleanupStrategy.AUTOMATIC, mapping.cleanup());
            assertEquals(List.of("env=staging"), mapping.namespaceLabels());
            assertEquals(List.of("deploy=auto"), mapping.namespaceAnnotations());
        }
    }

    @Nested
    @DisplayName("Create and Store Tests")
    class CreateAndStoreTests {

        @Test
        @DisplayName("Should throw exception when annotation is missing")
        void shouldThrowExceptionWhenAnnotationIsMissing() {
            // Given - ConfigurationManagerTest doesn't have @KubernetesTest annotation

            // When/Then - should throw exception since real class has no annotation
            IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                configurationManager.createAndStoreTestConfig(extensionContext));

            assertEquals("@KubernetesTest annotation not found on test class", exception.getMessage());
        }

        @Test
        @DisplayName("Should create TestConfig from annotation when present")
        void shouldCreateTestConfigFromAnnotationWhenPresent() {
            // Given
            KubernetesTest annotation = createMockKubernetesTestAnnotation();
            when(annotation.namespaces()).thenReturn(new String[]{"test-ns"});

            // When
            TestConfig result = configurationManager.createTestConfig(extensionContext, annotation);

            // Then
            assertNotNull(result);
            assertEquals(List.of("test-ns"), result.namespaces());
        }
    }

    @Nested
    @DisplayName("Namespace Generation Tests")
    class NamespaceGenerationTests {

        @Test
        @DisplayName("Should generate namespace with class name and timestamp")
        void shouldGenerateNamespaceWithClassNameAndTimestamp() {
            // When
            String namespace = configurationManager.generateNamespace(extensionContext);

            // Then
            assertTrue(namespace.startsWith("test-configurationmanagertest-"));
            assertTrue(namespace.length() > 30); // Should have timestamp suffix
            assertTrue(namespace.matches("test-configurationmanagertest-\\d{8}-\\d{6}"));
        }

        @Test
        @DisplayName("Should generate namespace with consistent format")
        void shouldGenerateNamespaceWithConsistentFormat() {
            // When
            String namespace1 = configurationManager.generateNamespace(extensionContext);
            String namespace2 = configurationManager.generateNamespace(extensionContext);

            // Then
            assertTrue(namespace1.startsWith("test-configurationmanagertest-"));
            assertTrue(namespace2.startsWith("test-configurationmanagertest-"));
            assertTrue(namespace1.matches("test-configurationmanagertest-\\d{8}-\\d{6}"));
            assertTrue(namespace2.matches("test-configurationmanagertest-\\d{8}-\\d{6}"));
            // Note: May be same if called within the same second, which is acceptable
        }
    }

    @Nested
    @DisplayName("Get TestConfig Tests")
    class GetTestConfigTests {

        @Test
        @DisplayName("Should get TestConfig from context store helper")
        void shouldGetTestConfigFromContextStoreHelper() {
            // Given
            TestConfig expectedConfig = new TestConfig(
                List.of("test-ns"), true, CleanupStrategy.AUTOMATIC, "", false, "",
                List.of(), List.of(), "#", 76, false, LogCollectionStrategy.ON_FAILURE,
                "", false, List.of("pods"), List.of(), List.of()
            );
            when(contextStoreHelper.getTestConfig(extensionContext)).thenReturn(expectedConfig);

            // When
            TestConfig result = configurationManager.getTestConfig(extensionContext);

            // Then
            assertEquals(expectedConfig, result);
            verify(contextStoreHelper).getTestConfig(extensionContext);
        }

        @Test
        @DisplayName("Should return null when TestConfig not found")
        void shouldReturnNullWhenTestConfigNotFound() {
            // Given
            when(contextStoreHelper.getTestConfig(extensionContext)).thenReturn(null);

            // When
            TestConfig result = configurationManager.getTestConfig(extensionContext);

            // Then
            assertNull(result);
        }
    }

    // Helper methods to create mock annotations
    private KubernetesTest createMockKubernetesTestAnnotation() {
        KubernetesTest annotation = mock(KubernetesTest.class);

        // Set up default return values for all annotation methods
        when(annotation.namespaces()).thenReturn(new String[0]);
        when(annotation.createNamespaces()).thenReturn(true);
        when(annotation.cleanup()).thenReturn(CleanupStrategy.AUTOMATIC);
        when(annotation.context()).thenReturn("");
        when(annotation.storeYaml()).thenReturn(false);
        when(annotation.yamlStorePath()).thenReturn("");
        when(annotation.namespaceLabels()).thenReturn(new String[0]);
        when(annotation.namespaceAnnotations()).thenReturn(new String[0]);
        when(annotation.visualSeparatorChar()).thenReturn("#");
        when(annotation.visualSeparatorLength()).thenReturn(76);
        when(annotation.collectLogs()).thenReturn(false);
        when(annotation.logCollectionStrategy()).thenReturn(LogCollectionStrategy.ON_FAILURE);
        when(annotation.logCollectionPath()).thenReturn("");
        when(annotation.collectPreviousLogs()).thenReturn(false);
        when(annotation.collectNamespacedResources()).thenReturn(new String[]{"pods"});
        when(annotation.collectClusterWideResources()).thenReturn(new String[0]);
        when(annotation.contextMappings()).thenReturn(new KubernetesTest.ContextMapping[0]);

        return annotation;
    }

    private KubernetesTest.ContextMapping createMockContextMappingAnnotation() {
        KubernetesTest.ContextMapping contextMapping = mock(KubernetesTest.ContextMapping.class);

        // Set up default return values
        when(contextMapping.context()).thenReturn("");
        when(contextMapping.namespaces()).thenReturn(new String[0]);
        when(contextMapping.createNamespaces()).thenReturn(true);
        when(contextMapping.cleanup()).thenReturn(CleanupStrategy.AUTOMATIC);
        when(contextMapping.namespaceLabels()).thenReturn(new String[0]);
        when(contextMapping.namespaceAnnotations()).thenReturn(new String[0]);

        return contextMapping;
    }
}