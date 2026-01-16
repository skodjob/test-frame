/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.junit;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.skodjob.testframe.clients.KubeClient;
import io.skodjob.testframe.clients.cmdClient.KubeCmdClient;
import io.skodjob.testframe.junit.annotations.CleanupStrategy;
import io.skodjob.testframe.junit.annotations.InjectCmdKubeClient;
import io.skodjob.testframe.junit.annotations.InjectKubeClient;
import io.skodjob.testframe.junit.annotations.InjectNamespace;
import io.skodjob.testframe.junit.annotations.InjectResource;
import io.skodjob.testframe.junit.annotations.InjectResourceManager;
import io.skodjob.testframe.junit.annotations.KubernetesTest;
import io.skodjob.testframe.junit.annotations.LogCollectionStrategy;
import io.skodjob.testframe.LogCollector;
import io.skodjob.testframe.LogCollectorBuilder;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.utils.LoggerUtils;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JUnit 5 extension for Kubernetes testing.
 * This extension provides automatic setup and teardown of Kubernetes resources,
 * dependency injection of Kubernetes clients, and namespace management.
 */
public class KubernetesTestExtension implements BeforeAllCallback, AfterAllCallback,
    BeforeEachCallback, AfterEachCallback, ParameterResolver, TestWatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesTestExtension.class);

    // Extension context store keys
    private static final String NAMESPACE_KEY = "kubernetes.test.namespace";
    private static final String RESOURCE_MANAGER_KEY = "kubernetes.test.resourceManager";
    private static final String TEST_CONFIG_KEY = "kubernetes.test.config";
    private static final String CREATED_NAMESPACE_KEY = "kubernetes.test.createdNamespace";
    private static final String LOG_COLLECTOR_KEY = "kubernetes.test.logCollector";

    // Store for managing resources across test lifecycle
    private static final Map<String, TestConfig> TEST_CONFIGS = new ConcurrentHashMap<>();

    /**
     * Default constructor for the Kubernetes test extension.
     * This extension is typically instantiated automatically by the JUnit 5 framework
     * when the {@link KubernetesTest} annotation is present on a test class.
     */
    public KubernetesTestExtension() {
        // Default constructor
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        logVisualSeparator(context);
        LOGGER.info("TestClass {} STARTED", context.getRequiredTestClass().getName());
        LOGGER.info("Setting up Kubernetes test environment for class: {}",
            context.getRequiredTestClass().getSimpleName());

        KubernetesTest testAnnotation = getKubernetesTestAnnotation(context);
        if (testAnnotation == null) {
            throw new IllegalStateException("@KubernetesTest annotation not found on test class");
        }

        TestConfig testConfig = createTestConfig(context, testAnnotation);
        TEST_CONFIGS.put(context.getUniqueId(), testConfig);
        context.getStore(ExtensionContext.Namespace.GLOBAL).put(TEST_CONFIG_KEY, testConfig);

        // Set up KubeResourceManager
        KubeResourceManager resourceManager = KubeResourceManager.get();

        // Configure context if specified
        if (!testConfig.context().isEmpty()) {
            AutoCloseable contextCloser = resourceManager.useContext(testConfig.context());
            context.getStore(ExtensionContext.Namespace.GLOBAL)
                .put("context.closer", contextCloser);
        }

        resourceManager.setTestContext(context);
        context.getStore(ExtensionContext.Namespace.GLOBAL)
            .put(RESOURCE_MANAGER_KEY, resourceManager);

        // Configure YAML storage if enabled
        if (testConfig.storeYaml()) {
            String yamlPath = testConfig.yamlStorePath().isEmpty() ?
                Paths.get("target", "test-yamls").toString() : testConfig.yamlStorePath();
            resourceManager.setStoreYamlPath(yamlPath);
        }

        // Set up namespace
        setupNamespace(context, testConfig, resourceManager);

        // Set up log collection if enabled
        if (testConfig.collectLogs()) {
            setupLogCollector(context, testConfig, resourceManager);
        }

        // Inject fields for PER_CLASS lifecycle tests that use @BeforeAll methods
        injectTestClassFields(context, testConfig);

        logVisualSeparator(context);
        LOGGER.info("Kubernetes test environment ready for: {}",
            context.getRequiredTestClass().getSimpleName());
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        LOGGER.info("Cleaning up Kubernetes test environment for class: {}",
            context.getRequiredTestClass().getSimpleName());

        TestConfig testConfig = getTestConfig(context);
        if (testConfig == null) {
            return;
        }

        KubeResourceManager resourceManager = getResourceManager(context);
        if (resourceManager != null && testConfig.cleanup() == CleanupStrategy.AFTER_ALL) {
            resourceManager.deleteResources();
        }

        // Collect logs if configured for AFTER_ALL
        if (testConfig.collectLogs() && testConfig.logCollectionStrategy() == LogCollectionStrategy.AFTER_ALL) {
            collectLogs(context, "after-all");
        }

        // Clean up created namespace if we created it
        if (testConfig.createNamespace() &&
            context.getStore(ExtensionContext.Namespace.GLOBAL).get(CREATED_NAMESPACE_KEY) != null) {
            cleanupNamespace(context, testConfig);
        }

        // Close context if we opened it
        AutoCloseable contextCloser = context.getStore(ExtensionContext.Namespace.GLOBAL)
            .get("context.closer", AutoCloseable.class);
        if (contextCloser != null) {
            contextCloser.close();
        }

        TEST_CONFIGS.remove(context.getUniqueId());
        LOGGER.info("TestClass {} FINISHED", context.getRequiredTestClass().getName());
        logVisualSeparator(context);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        TestConfig testConfig = getTestConfig(context);
        if (testConfig == null) {
            return;
        }

        KubeResourceManager resourceManager = getResourceManager(context);
        if (resourceManager != null) {
            resourceManager.setTestContext(context);
        }

        // Inject fields into the current test instance
        injectTestClassFields(context, testConfig);

        logVisualSeparator(context);
        LOGGER.info("Test {}.{} STARTED", context.getRequiredTestClass().getName(),
            context.getDisplayName().replace("()", ""));
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        TestConfig testConfig = getTestConfig(context);
        if (testConfig == null) {
            return;
        }

        KubeResourceManager resourceManager = getResourceManager(context);
        if (resourceManager != null && testConfig.cleanup() == CleanupStrategy.AFTER_EACH) {
            resourceManager.deleteResources();
        }

        String state = "SUCCEEDED";
        if (context.getExecutionException().isPresent()) {
            state = "FAILED";
        }

        // Collect logs if configured for AFTER_EACH
        if (testConfig.collectLogs() && testConfig.logCollectionStrategy() == LogCollectionStrategy.AFTER_EACH) {
            String testName = context.getDisplayName().replace("()", "");
            collectLogs(context, "after-each-" + testName.toLowerCase());
        }

        LOGGER.info("Test {}.{} {}", context.getRequiredTestClass().getName(),
            context.getDisplayName().replace("()", ""), state);
        logVisualSeparator(context);
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        TestConfig testConfig = getTestConfig(context);
        if (testConfig == null) {
            return;
        }

        LOGGER.error("Test failed: {}", context.getDisplayName(), cause);

        // Collect logs if configured for ON_FAILURE
        if (testConfig.collectLogs() && testConfig.logCollectionStrategy() == LogCollectionStrategy.ON_FAILURE) {
            String testName = context.getDisplayName().replace("()", "");
            collectLogs(context, "on-failure-" + testName.toLowerCase());
        }

        KubeResourceManager resourceManager = getResourceManager(context);
        if (resourceManager != null && testConfig.cleanup() == CleanupStrategy.ON_FAILURE) {
            LOGGER.info("Cleaning up resources due to test failure");
            resourceManager.deleteResources();
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext,
                                     ExtensionContext extensionContext) throws ParameterResolutionException {
        return hasInjectAnnotation(parameterContext);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext,
                                   ExtensionContext extensionContext) throws ParameterResolutionException {
        return injectParameter(parameterContext, extensionContext);
    }

    private KubernetesTest getKubernetesTestAnnotation(ExtensionContext context) {
        return context.getRequiredTestClass().getAnnotation(KubernetesTest.class);
    }

    private TestConfig createTestConfig(ExtensionContext context, KubernetesTest annotation) {
        String namespace = annotation.namespace().isEmpty() ?
            generateNamespace(context) : annotation.namespace();

        return new TestConfig(
            namespace,
            annotation.createNamespace(),
            annotation.cleanup(),
            annotation.context(),
            annotation.storeYaml(),
            annotation.yamlStorePath(),
            annotation.namespaceLabels(),
            annotation.namespaceAnnotations(),
            annotation.visualSeparatorChar(),
            annotation.visualSeparatorLength(),
            annotation.collectLogs(),
            annotation.logCollectionStrategy(),
            annotation.logCollectionPath(),
            annotation.collectPreviousLogs(),
            annotation.collectNamespacedResources(),
            annotation.collectClusterWideResources(),
            annotation.collectEvents()
        );
    }

    private String generateNamespace(ExtensionContext context) {
        String className = context.getRequiredTestClass().getSimpleName().toLowerCase();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        return String.format("test-%s-%s", className, timestamp);
    }

    private void setupNamespace(ExtensionContext context, TestConfig testConfig,
                                KubeResourceManager resourceManager) {
        String namespaceName = testConfig.namespace();

        if (testConfig.createNamespace()) {
            LOGGER.info("Creating test namespace: {}", namespaceName);

            // Start with basic namespace
            var metadataBuilder = new NamespaceBuilder()
                .withNewMetadata()
                .withName(namespaceName);

            // Add labels
            for (String label : testConfig.namespaceLabels()) {
                String[] parts = label.split("=", 2);
                if (parts.length == 2) {
                    metadataBuilder.addToLabels(parts[0], parts[1]);
                }
            }

            // Add annotations
            for (String annotation : testConfig.namespaceAnnotations()) {
                String[] parts = annotation.split("=", 2);
                if (parts.length == 2) {
                    metadataBuilder.addToAnnotations(parts[0], parts[1]);
                }
            }

            Namespace namespace = metadataBuilder.endMetadata().build();

            resourceManager.createResourceWithWait(namespace);
            context.getStore(ExtensionContext.Namespace.GLOBAL).put(CREATED_NAMESPACE_KEY, true);
        }

        context.getStore(ExtensionContext.Namespace.GLOBAL).put(NAMESPACE_KEY, namespaceName);
        LOGGER.info("Using namespace: {}", namespaceName);
    }

    private void cleanupNamespace(ExtensionContext context, TestConfig testConfig) {
        String namespaceName = testConfig.namespace();
        LOGGER.info("Deleting test namespace: {}", namespaceName);

        try {
            KubeResourceManager resourceManager = getResourceManager(context);
            if (resourceManager != null) {
                Namespace namespace = new NamespaceBuilder()
                    .withNewMetadata()
                    .withName(namespaceName)
                    .endMetadata()
                    .build();
                resourceManager.deleteResourceWithWait(namespace);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to delete namespace: {}", namespaceName, e);
        }
    }

    private void injectTestClassFields(ExtensionContext context, TestConfig testConfig) {
        Object testInstance = context.getTestInstance().orElse(null);
        if (testInstance == null) {
            return;
        }

        Field[] fields = context.getRequiredTestClass().getDeclaredFields();
        for (Field field : fields) {
            try {
                Object value = getInjectableValue(field, context);
                if (value != null) {
                    field.setAccessible(true);
                    field.set(testInstance, value);
                    LOGGER.debug("Injected {} into field: {}", value.getClass().getSimpleName(), field.getName());
                }
            } catch (Exception e) {
                LOGGER.error("Failed to inject field: {}", field.getName(), e);
                throw new RuntimeException("Field injection failed for: " + field.getName(), e);
            }
        }
    }

    private boolean hasInjectAnnotation(ParameterContext parameterContext) {
        return parameterContext.isAnnotated(InjectKubeClient.class) ||
               parameterContext.isAnnotated(InjectCmdKubeClient.class) ||
               parameterContext.isAnnotated(InjectResourceManager.class) ||
               parameterContext.isAnnotated(InjectNamespace.class) ||
               parameterContext.isAnnotated(InjectResource.class);
    }

    private Object injectParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        if (parameterContext.isAnnotated(InjectKubeClient.class)) {
            return injectKubeClient(parameterContext, extensionContext);
        } else if (parameterContext.isAnnotated(InjectCmdKubeClient.class)) {
            return injectCmdKubeClient(parameterContext, extensionContext);
        } else if (parameterContext.isAnnotated(InjectResourceManager.class)) {
            return getResourceManager(extensionContext);
        } else if (parameterContext.isAnnotated(InjectNamespace.class)) {
            return injectNamespace(parameterContext, extensionContext);
        } else if (parameterContext.isAnnotated(InjectResource.class)) {
            return injectResource(parameterContext, extensionContext);
        }

        throw new ParameterResolutionException("Cannot resolve parameter: " + parameterContext.getParameter());
    }

    private Object getInjectableValue(Field field, ExtensionContext context) {
        if (field.isAnnotationPresent(InjectKubeClient.class)) {
            return injectKubeClientForField(field, context);
        } else if (field.isAnnotationPresent(InjectCmdKubeClient.class)) {
            return injectCmdKubeClientForField(field, context);
        } else if (field.isAnnotationPresent(InjectResourceManager.class)) {
            return getResourceManager(context);
        } else if (field.isAnnotationPresent(InjectNamespace.class)) {
            return injectNamespaceForField(field, context);
        } else if (field.isAnnotationPresent(InjectResource.class)) {
            return injectResourceForField(field, context);
        }

        return null;
    }

    private KubeClient injectKubeClient(ParameterContext parameterContext, ExtensionContext extensionContext) {
        KubeResourceManager resourceManager = getResourceManager(extensionContext);
        if (resourceManager == null) {
            throw new ParameterResolutionException("KubeResourceManager not available");
        }
        return resourceManager.kubeClient();
    }

    private KubeClient injectCmdKubeClient(ParameterContext parameterContext, ExtensionContext extensionContext) {
        KubeResourceManager resourceManager = getResourceManager(extensionContext);
        if (resourceManager == null) {
            throw new ParameterResolutionException("KubeResourceManager not available");
        }
        return resourceManager.kubeCmdClient();
    }

    private KubeClient injectKubeClientForField(Field field, ExtensionContext context) {
        KubeResourceManager resourceManager = getResourceManager(context);
        if (resourceManager == null) {
            throw new RuntimeException("KubeResourceManager not available");
        }
        return resourceManager.kubeClient();
    }

    private KubeCmdClient<?> injectCmdKubeClientForField(Field field, ExtensionContext context) {
        KubeResourceManager resourceManager = getResourceManager(context);
        if (resourceManager == null) {
            throw new RuntimeException("KubeResourceManager not available");
        }
        return resourceManager.kubeCmdClient();
    }

    private Object injectNamespace(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Class<?> parameterType = parameterContext.getParameter().getType();
        return getNamespaceValue(parameterType, extensionContext);
    }

    private Object injectNamespaceForField(Field field, ExtensionContext context) {
        Class<?> fieldType = field.getType();
        return getNamespaceValue(fieldType, context);
    }

    private Object getNamespaceValue(Class<?> targetType, ExtensionContext context) {
        String namespaceName = context.getStore(ExtensionContext.Namespace.GLOBAL)
            .get(NAMESPACE_KEY, String.class);

        if (targetType == String.class) {
            return namespaceName;
        } else if (targetType == Namespace.class) {
            KubeResourceManager resourceManager = getResourceManager(context);
            if (resourceManager != null) {
                return resourceManager.kubeClient().getClient()
                    .namespaces().withName(namespaceName).get();
            }
        }

        throw new RuntimeException("Unsupported namespace injection type: " + targetType);
    }

    private Object injectResource(ParameterContext parameterContext, ExtensionContext extensionContext) {
        InjectResource annotation = parameterContext.getParameter().getAnnotation(InjectResource.class);
        return loadAndInjectResource(annotation, parameterContext.getParameter().getType(), extensionContext);
    }

    private Object injectResourceForField(Field field, ExtensionContext context) {
        InjectResource annotation = field.getAnnotation(InjectResource.class);
        return loadAndInjectResource(annotation, field.getType(), context);
    }

    @SuppressWarnings("unchecked")
    private <T extends HasMetadata> T loadAndInjectResource(InjectResource annotation,
                                                            Class<?> targetType,
                                                            ExtensionContext context) {
        try {
            KubeResourceManager resourceManager = getResourceManager(context);
            if (resourceManager == null) {
                throw new RuntimeException("KubeResourceManager not available");
            }

            // Load resource from file
            List<HasMetadata> resources = resourceManager.readResourcesFromFile(
                Paths.get(annotation.value()));

            if (resources.isEmpty()) {
                throw new RuntimeException("No resources found in file: " + annotation.value());
            }

            // Find resource of matching type
            HasMetadata resource = resources.stream()
                .filter(r -> targetType.isAssignableFrom(r.getClass()))
                .findFirst()
                .orElse(resources.get(0)); // Fallback to first resource


            // Apply resource to cluster (use createOrUpdate to handle existing resources)
            if (annotation.waitForReady()) {
                resourceManager.createOrUpdateResourceWithWait(resource);
            } else {
                resourceManager.createOrUpdateResourceWithoutWait(resource);
            }

            return (T) resource;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load resource from: " + annotation.value(), e);
        }
    }

    private TestConfig getTestConfig(ExtensionContext context) {
        return context.getStore(ExtensionContext.Namespace.GLOBAL).get(TEST_CONFIG_KEY, TestConfig.class);
    }

    private KubeResourceManager getResourceManager(ExtensionContext context) {
        return context.getStore(ExtensionContext.Namespace.GLOBAL)
            .get(RESOURCE_MANAGER_KEY, KubeResourceManager.class);
    }

    private void logVisualSeparator(ExtensionContext context) {
        TestConfig testConfig = getTestConfig(context);
        if (testConfig != null) {
            LoggerUtils.logSeparator(testConfig.visualSeparatorChar(), testConfig.visualSeparatorLength());
        } else {
            LoggerUtils.logSeparator();
        }
    }

    private void setupLogCollector(ExtensionContext context, TestConfig testConfig,
                                   KubeResourceManager resourceManager) {
        LOGGER.info("Setting up log collector with strategy: {}", testConfig.logCollectionStrategy());

        String logPath = testConfig.logCollectionPath().isEmpty() ?
            "target/test-logs" : testConfig.logCollectionPath();

        LogCollectorBuilder builder = new LogCollectorBuilder()
            .withRootFolderPath(logPath)
            .withKubeClient(resourceManager.kubeClient())
            .withNamespacedResources(testConfig.collectNamespacedResources());

        if (testConfig.collectClusterWideResources().length > 0) {
            builder.withClusterWideResources(testConfig.collectClusterWideResources());
        }

        if (testConfig.collectPreviousLogs()) {
            builder.withCollectPreviousLogs();
        }

        LogCollector logCollector = builder.build();
        context.getStore(ExtensionContext.Namespace.GLOBAL).put(LOG_COLLECTOR_KEY, logCollector);

        LOGGER.info("Log collector configured to collect to: {}", logPath);
    }

    private void collectLogs(ExtensionContext context, String suffix) {
        TestConfig testConfig = getTestConfig(context);
        LogCollector logCollector = getLogCollector(context);

        if (logCollector == null) {
            LOGGER.warn("Log collector not available, skipping log collection");
            return;
        }

        try {
            LOGGER.info("Collecting logs for test failure/completion: {}", suffix);
            String namespace = testConfig.namespace();

            // Create a subfolder for this specific collection
            String testClassName = context.getRequiredTestClass().getSimpleName().toLowerCase();
            String logPath = String.format("%s/%s/%s",
                testConfig.logCollectionPath().isEmpty() ? "target/test-logs" : testConfig.logCollectionPath(),
                testClassName,
                suffix);

            // Collect logs from the test namespace
            logCollector.collectFromNamespaces(namespace);

            // Collect events if configured
            if (testConfig.collectEvents()) {
                logCollector.collectEventsFromNamespace(namespace, logPath);
            }

            LOGGER.info("Log collection completed successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to collect logs", e);
        }
    }

    private LogCollector getLogCollector(ExtensionContext context) {
        return context.getStore(ExtensionContext.Namespace.GLOBAL)
            .get(LOG_COLLECTOR_KEY, LogCollector.class);
    }

    /**
     * Configuration holder for test setup.
     *
     * @param namespace The Kubernetes namespace to use for testing
     * @param createNamespace Whether to create the namespace if it doesn't exist
     * @param cleanup The cleanup strategy for resources
     * @param context The Kubernetes cluster context to use
     * @param storeYaml Whether to store YAML representations of resources
     * @param yamlStorePath Directory path to store YAML files
     * @param namespaceLabels Labels to apply to the test namespace
     * @param namespaceAnnotations Annotations to apply to the test namespace
     * @param visualSeparatorChar Character to use for visual separators
     * @param visualSeparatorLength Length of visual separator lines
     * @param collectLogs Whether log collection is enabled
     * @param logCollectionStrategy When to collect logs
     * @param logCollectionPath Directory path for log collection
     * @param collectPreviousLogs Whether to collect previous container logs
     * @param collectNamespacedResources Namespaced resource types to collect
     * @param collectClusterWideResources Cluster-wide resource types to collect
     * @param collectEvents Whether to collect events
     */
    private record TestConfig(
        String namespace,
        boolean createNamespace,
        CleanupStrategy cleanup,
        String context,
        boolean storeYaml,
        String yamlStorePath,
        String[] namespaceLabels,
        String[] namespaceAnnotations,
        String visualSeparatorChar,
        int visualSeparatorLength,
        boolean collectLogs,
        LogCollectionStrategy logCollectionStrategy,
        String logCollectionPath,
        boolean collectPreviousLogs,
        String[] collectNamespacedResources,
        String[] collectClusterWideResources,
        boolean collectEvents
    ) { }
}