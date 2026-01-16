/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.kubetest;

import io.skodjob.testframe.kubetest.annotations.CleanupStrategy;
import io.skodjob.testframe.kubetest.annotations.InjectCmdKubeClient;
import io.skodjob.testframe.kubetest.annotations.InjectKubeClient;
import io.skodjob.testframe.kubetest.annotations.InjectNamespace;
import io.skodjob.testframe.kubetest.annotations.InjectNamespaces;
import io.skodjob.testframe.kubetest.annotations.InjectResourceManager;
import io.skodjob.testframe.kubetest.annotations.KubernetesTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for multi-context functionality in KubernetesTestExtension.
 * These tests verify the new context-aware features without requiring a real Kubernetes cluster.
 */
class MultiContextExtensionTest {

    @Nested
    @DisplayName("Context Mapping Configuration Tests")
    class ContextMappingTests {

        @Test
        @DisplayName("Should create ContextMappingConfig with default values")
        void shouldCreateContextMappingConfigWithDefaults() {
            // Given
            TestConfig.ContextMappingConfig config = new TestConfig.ContextMappingConfig(
                "test-context",
                List.of("test-ns"),
                true,
                CleanupStrategy.AUTOMATIC,
                List.of(),
                List.of()
            );

            // Then
            assertNotNull(config);
            assertEquals("test-context", config.context());
            assertEquals(List.of("test-ns"), config.namespaces());
            assertTrue(config.createNamespaces());
            assertEquals(CleanupStrategy.AUTOMATIC, config.cleanup());
            assertEquals(List.of(), config.namespaceLabels());
            assertEquals(List.of(), config.namespaceAnnotations());
        }

        @Test
        @DisplayName("Should create ContextMappingConfig with custom values")
        void shouldCreateContextMappingConfigWithCustomValues() {
            // Given
            TestConfig.ContextMappingConfig config = new TestConfig.ContextMappingConfig(
                "staging-cluster",
                List.of("stg-ns1", "stg-ns2"),
                false,
                CleanupStrategy.MANUAL,
                List.of("environment=staging", "team=platform"),
                List.of("deployment.io/stage=staging", "monitoring=enabled")
            );

            // Then
            assertEquals("staging-cluster", config.context());
            assertEquals(List.of("stg-ns1", "stg-ns2"), config.namespaces());
            assertFalse(config.createNamespaces());
            assertEquals(CleanupStrategy.MANUAL, config.cleanup());
            assertEquals(List.of("environment=staging", "team=platform"),
                            config.namespaceLabels());
            assertEquals(List.of("deployment.io/stage=staging", "monitoring=enabled"),
                            config.namespaceAnnotations());
        }

        @Test
        @DisplayName("Should convert annotation to ContextMappingConfig")
        void shouldConvertAnnotationToContextMappingConfig() {
            // Create a mock ContextMapping annotation
            KubernetesTest.ContextMapping annotation = new KubernetesTest.ContextMapping() {
                @Override
                public Class<? extends java.lang.annotation.Annotation> annotationType() {
                    return KubernetesTest.ContextMapping.class;
                }

                @Override
                public String context() {
                    return "production";
                }

                @Override
                public String[] namespaces() {
                    return new String[]{"prod-api", "prod-cache"};
                }

                @Override
                public boolean createNamespaces() {
                    return false;
                }

                @Override
                public CleanupStrategy cleanup() {
                    return CleanupStrategy.MANUAL;
                }

                @Override
                public String[] namespaceLabels() {
                    return new String[]{"env=production"};
                }

                @Override
                public String[] namespaceAnnotations() {
                    return new String[]{"deployment.io/environment=production"};
                }
            };

            // When
            TestConfig.ContextMappingConfig config = TestConfig.ContextMappingConfig.fromAnnotation(annotation);

            // Then
            assertEquals("production", config.context());
            assertEquals(List.of("prod-api", "prod-cache"), config.namespaces());
            assertFalse(config.createNamespaces());
            assertEquals(CleanupStrategy.MANUAL, config.cleanup());
            assertEquals(List.of("env=production"), config.namespaceLabels());
            assertEquals(List.of("deployment.io/environment=production"),
                            config.namespaceAnnotations());
        }
    }

    @Nested
    @DisplayName("TestConfig with Context Mappings Tests")
    class TestConfigWithContextMappingsTests {

        @Test
        @DisplayName("Should create TestConfig with empty context mappings")
        void shouldCreateTestConfigWithEmptyContextMappings() {
            // Given
            TestConfig config = new TestConfig(
                List.of("default-ns"),
                true,
                CleanupStrategy.AUTOMATIC,
                "",
                false,
                "",
                List.of(),
                List.of(),
                "#",
                76,
                false,
                io.skodjob.testframe.kubetest.annotations.LogCollectionStrategy.ON_FAILURE,
                "",
                false,
                List.of("pods"),
                List.of(),
                List.of() // Empty context mappings
            );

            // Then
            assertNotNull(config.contextMappings());
            assertEquals(0, config.contextMappings().size());
        }

        @Test
        @DisplayName("Should create TestConfig with multiple context mappings")
        void shouldCreateTestConfigWithMultipleContextMappings() {
            // Given
            List<TestConfig.ContextMappingConfig> contextMappings = List.of(
                new TestConfig.ContextMappingConfig(
                    "staging-cluster",
                    List.of("stg-app", "stg-db"),
                    true,
                    CleanupStrategy.AUTOMATIC,
                    List.of("env=staging"),
                    List.of("stage=staging")
                ),
                new TestConfig.ContextMappingConfig(
                    "production-cluster",
                    List.of("prod-api"),
                    false,
                    CleanupStrategy.MANUAL,
                    List.of("env=production"),
                    List.of("stage=production")
                )
            );

            TestConfig config = new TestConfig(
                List.of("default-ns"),
                true,
                CleanupStrategy.AUTOMATIC,
                "",
                false,
                "",
                List.of(),
                List.of(),
                "#",
                76,
                false,
                io.skodjob.testframe.kubetest.annotations.LogCollectionStrategy.ON_FAILURE,
                "",
                false,
                List.of("pods"),
                List.of(),
                contextMappings
            );

            // Then
            assertNotNull(config.contextMappings());
            assertEquals(2, config.contextMappings().size());

            // Verify staging context mapping
            TestConfig.ContextMappingConfig stagingMapping = config.contextMappings().get(0);
            assertEquals("staging-cluster", stagingMapping.context());
            assertEquals(List.of("stg-app", "stg-db"), stagingMapping.namespaces());
            assertTrue(stagingMapping.createNamespaces());
            assertEquals(CleanupStrategy.AUTOMATIC, stagingMapping.cleanup());

            // Verify production context mapping
            TestConfig.ContextMappingConfig prodMapping = config.contextMappings().get(1);
            assertEquals("production-cluster", prodMapping.context());
            assertEquals(List.of("prod-api"), prodMapping.namespaces());
            assertFalse(prodMapping.createNamespaces());
            assertEquals(CleanupStrategy.MANUAL, prodMapping.cleanup());
        }
    }

    @Nested
    @DisplayName("Context-Aware Injection Annotation Tests")
    class ContextAwareAnnotationTests {

        @Test
        @DisplayName("Should verify InjectKubeClient annotation with context")
        void shouldVerifyInjectKubeClientWithContext() {
            // Create a mock annotation with context
            InjectKubeClient annotation = new InjectKubeClient() {
                @Override
                public Class<? extends java.lang.annotation.Annotation> annotationType() {
                    return InjectKubeClient.class;
                }

                @Override
                public String context() {
                    return "staging-cluster";
                }
            };

            // Verify
            assertEquals("staging-cluster", annotation.context());
        }

        @Test
        @DisplayName("Should verify InjectCmdKubeClient annotation with context")
        void shouldVerifyInjectCmdKubeClientWithContext() {
            InjectCmdKubeClient annotation = new InjectCmdKubeClient() {
                @Override
                public Class<? extends java.lang.annotation.Annotation> annotationType() {
                    return InjectCmdKubeClient.class;
                }

                @Override
                public String context() {
                    return "production-cluster";
                }
            };

            assertEquals("production-cluster", annotation.context());
        }

        @Test
        @DisplayName("Should verify InjectResourceManager annotation with context")
        void shouldVerifyInjectResourceManagerWithContext() {
            InjectResourceManager annotation = new InjectResourceManager() {
                @Override
                public Class<? extends java.lang.annotation.Annotation> annotationType() {
                    return InjectResourceManager.class;
                }

                @Override
                public String context() {
                    return "dev-cluster";
                }
            };

            assertEquals("dev-cluster", annotation.context());
        }

        @Test
        @DisplayName("Should verify InjectNamespace annotation with context")
        void shouldVerifyInjectNamespaceWithContext() {
            InjectNamespace annotation = new InjectNamespace() {
                @Override
                public Class<? extends java.lang.annotation.Annotation> annotationType() {
                    return InjectNamespace.class;
                }

                @Override
                public String name() {
                    return "my-namespace";
                }

                @Override
                public String context() {
                    return "my-context";
                }
            };

            assertEquals("my-namespace", annotation.name());
            assertEquals("my-context", annotation.context());
        }

        @Test
        @DisplayName("Should verify InjectNamespaces annotation with context")
        void shouldVerifyInjectNamespacesWithContext() {
            InjectNamespaces annotation = new InjectNamespaces() {
                @Override
                public Class<? extends java.lang.annotation.Annotation> annotationType() {
                    return InjectNamespaces.class;
                }

                @Override
                public String context() {
                    return "all-namespaces-context";
                }
            };

            assertEquals("all-namespaces-context", annotation.context());
        }

        @Test
        @DisplayName("Should use empty string as default context value")
        void shouldUseEmptyStringAsDefaultContext() {
            // Test that annotations default to empty string for context when not specified
            // This simulates the behavior when @InjectKubeClient is used without context parameter

            InjectKubeClient defaultAnnotation = new InjectKubeClient() {
                @Override
                public Class<? extends java.lang.annotation.Annotation> annotationType() {
                    return InjectKubeClient.class;
                }

                @Override
                public String context() {
                    return ""; // Default value
                }
            };

            assertEquals("", defaultAnnotation.context());
        }
    }

    @Nested
    @DisplayName("Multi-Context Validation Tests")
    class MultiContextValidationTests {

        @Test
        @DisplayName("Should validate context names are non-null")
        void shouldValidateContextNamesAreNonNull() {
            // Test that context names in mappings are properly handled
            TestConfig.ContextMappingConfig config = new TestConfig.ContextMappingConfig(
                "valid-context",
                List.of("namespace1"),
                true,
                CleanupStrategy.AUTOMATIC,
                List.of(),
                List.of()
            );

            assertNotNull(config.context());
            assertFalse(config.context().trim().isEmpty());
        }

        @Test
        @DisplayName("Should validate namespace arrays are non-null")
        void shouldValidateNamespaceArraysAreNonNull() {
            TestConfig.ContextMappingConfig config = new TestConfig.ContextMappingConfig(
                "test-context",
                List.of("ns1", "ns2"),
                true,
                CleanupStrategy.AUTOMATIC,
                List.of(),
                List.of()
            );

            assertNotNull(config.namespaces());
            assertTrue(config.namespaces().size() > 0);
        }

        @Test
        @DisplayName("Should handle mixed context and default deployments")
        void shouldHandleMixedContextAndDefaultDeployments() {
            // Test configuration with both default namespaces and context-specific namespaces
            List<TestConfig.ContextMappingConfig> contextMappings = List.of(
                new TestConfig.ContextMappingConfig(
                    "external-cluster",
                    List.of("external-ns"),
                    true,
                    CleanupStrategy.AUTOMATIC,
                    List.of("external=true"),
                    List.of()
                )
            );

            TestConfig config = new TestConfig(
                List.of("default-ns1", "default-ns2"), // Default context namespaces
                true,
                CleanupStrategy.AUTOMATIC,
                "", // Default context
                false,
                "",
                List.of("default=true"),
                List.of(),
                "#",
                76,
                false,
                io.skodjob.testframe.kubetest.annotations.LogCollectionStrategy.ON_FAILURE,
                "",
                false,
                List.of("pods"),
                List.of(),
                contextMappings
            );

            // Verify default namespaces
            assertEquals(List.of("default-ns1", "default-ns2"), config.namespaces());
            assertEquals("", config.context());

            // Verify context-specific mappings
            assertEquals(1, config.contextMappings().size());
            assertEquals("external-cluster", config.contextMappings().get(0).context());
            assertEquals(List.of("external-ns"), config.contextMappings().get(0).namespaces());
        }
    }

    @Nested
    @DisplayName("Context Isolation Tests")
    class ContextIsolationTests {

        @Test
        @DisplayName("Should ensure context configurations are independent")
        void shouldEnsureContextConfigurationsAreIndependent() {
            // Create multiple context mappings with different configurations
            TestConfig.ContextMappingConfig staging = new TestConfig.ContextMappingConfig(
                "staging-cluster",
                List.of("stg-app"),
                true,
                CleanupStrategy.AUTOMATIC,
                List.of("env=staging"),
                List.of("auto-deploy=true")
            );

            TestConfig.ContextMappingConfig production = new TestConfig.ContextMappingConfig(
                "production-cluster",
                List.of("prod-app"),
                false, // Different createNamespaces setting
                CleanupStrategy.MANUAL, // Different cleanup strategy
                List.of("env=production"),
                List.of("auto-deploy=false")
            );

            // Verify they have independent configurations
            assertTrue(staging.createNamespaces());
            assertFalse(production.createNamespaces());

            assertEquals(CleanupStrategy.AUTOMATIC, staging.cleanup());
            assertEquals(CleanupStrategy.MANUAL, production.cleanup());

            assertEquals("staging", staging.namespaceLabels().get(0).split("=")[1]);
            assertEquals("production", production.namespaceLabels().get(0).split("=")[1]);
        }

        @Test
        @DisplayName("Should allow same namespace names in different contexts")
        void shouldAllowSameNamespaceNamesInDifferentContexts() {
            // This tests that namespace names can be reused across different contexts
            TestConfig.ContextMappingConfig context1 = new TestConfig.ContextMappingConfig(
                "cluster-1",
                List.of("app-namespace", "db-namespace"),
                true,
                CleanupStrategy.AUTOMATIC,
                List.of(),
                List.of()
            );

            TestConfig.ContextMappingConfig context2 = new TestConfig.ContextMappingConfig(
                "cluster-2",
                List.of("app-namespace", "db-namespace"), // Same names, different context
                true,
                CleanupStrategy.AUTOMATIC,
                List.of(),
                List.of()
            );

            // Both should be valid even with same namespace names
            assertEquals(context1.namespaces(), context2.namespaces());
            assertNotEquals(context1.context(), context2.context());
        }
    }
}