/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.kubetest;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.skodjob.testframe.LogCollector;
import io.skodjob.testframe.LogCollectorBuilder;
import io.skodjob.testframe.clients.KubeClient;
import io.skodjob.testframe.clients.cmdClient.KubeCmdClient;
import io.skodjob.testframe.kubetest.annotations.CleanupStrategy;
import io.skodjob.testframe.kubetest.annotations.InjectCmdKubeClient;
import io.skodjob.testframe.kubetest.annotations.InjectKubeClient;
import io.skodjob.testframe.kubetest.annotations.InjectNamespace;
import io.skodjob.testframe.kubetest.annotations.InjectNamespaces;
import io.skodjob.testframe.kubetest.annotations.InjectResource;
import io.skodjob.testframe.kubetest.annotations.InjectResourceManager;
import io.skodjob.testframe.kubetest.annotations.KubernetesTest;
import io.skodjob.testframe.kubetest.annotations.LogCollectionStrategy;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.utils.KubeUtils;
import io.skodjob.testframe.utils.LoggerUtils;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JUnit 5 extension for Kubernetes testing.
 * This extension provides automatic setup and teardown of Kubernetes resources,
 * dependency injection of Kubernetes clients, namespace management, and comprehensive
 * log collection.
 * <p>
 * Key features:
 * - Multi-namespace support with automatic labeling for log collection
 * - Comprehensive log collection via exception handlers (catches failures in ANY test phase)
 * - Automatic resource cleanup with configurable strategies
 * - Dependency injection for Kubernetes clients and resources
 * <p>
 * Log collection is triggered by exception handlers that catch failures from:
 * - beforeAll methods
 * - beforeEach methods
 * - test execution
 * - afterEach methods
 * - afterAll methods
 * <p>
 * This ensures no test failures are missed regardless of where they occur in the
 * test lifecycle, which is superior to manual log collection in specific methods.
 */
public class KubernetesTestExtension implements BeforeAllCallback, AfterAllCallback,
    BeforeEachCallback, AfterEachCallback, ParameterResolver, TestWatcher,
    TestExecutionExceptionHandler, LifecycleMethodExecutionExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesTestExtension.class);

    // Extension context store keys
    private static final String NAMESPACE_KEY = "kubernetes.test.namespace";
    private static final String NAMESPACE_OBJECTS_KEY = "kubernetes.test.namespaceObjects";
    private static final String RESOURCE_MANAGER_KEY = "kubernetes.test.resourceManager";
    private static final String TEST_CONFIG_KEY = "kubernetes.test.config";
    private static final String CREATED_NAMESPACE_KEY = "kubernetes.test.createdNamespace";
    private static final String CREATED_NAMESPACE_NAMES_KEY = "kubernetes.test.createdNamespaceNames";
    private static final String LOG_COLLECTOR_KEY = "kubernetes.test.logCollector";

    // Log collection labels
    private static final String LOG_COLLECTION_LABEL_KEY = "test-frame.io/log-collection";
    private static final String LOG_COLLECTION_LABEL_VALUE = "enabled";


    /**
     * Package-private constructor for testing purposes only.
     * This constructor should not be used in production code.
     */
    KubernetesTestExtension() {
        KubeResourceManager.get();
    }

    @Override
    public void beforeAll(@NonNull ExtensionContext context) throws Exception {
        logVisualSeparator(context);
        LOGGER.info("TestClass {} STARTED", context.getRequiredTestClass().getName());
        LOGGER.info("Setting up Kubernetes test environment for class: {}",
            context.getRequiredTestClass().getSimpleName());

        KubernetesTest testAnnotation = getKubernetesTestAnnotation(context);
        if (testAnnotation == null) {
            throw new IllegalStateException("@KubernetesTest annotation not found on test class");
        }

        TestConfig testConfig = createTestConfig(context, testAnnotation);
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

        // Set up log collection callback BEFORE creating namespaces
        if (testConfig.collectLogs()) {
            setupLogCollector(context, testConfig, resourceManager);
            setupNamespaceAutoLabeling(resourceManager);
        }

        // Set up namespaces (this will trigger the auto-labeling callback)
        setupNamespaces(context, testConfig, resourceManager);

        // Inject fields for PER_CLASS lifecycle tests that use @BeforeAll methods
        injectTestClassFields(context);

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

        // Handle cleanup by delegating to ResourceManager logic
        handleAutomaticCleanup(context, testConfig);

        // Clean up created namespaces if any were created during the test
        @SuppressWarnings("unchecked")
        List<String> createdNamespaces = (List<String>) context.getStore(ExtensionContext.Namespace.GLOBAL)
            .get(CREATED_NAMESPACE_NAMES_KEY);
        if (createdNamespaces != null && !createdNamespaces.isEmpty()) {
            cleanupNamespaces(context, testConfig);
        }

        // Close context if we opened it
        AutoCloseable contextCloser = context.getStore(ExtensionContext.Namespace.GLOBAL)
            .get("context.closer", AutoCloseable.class);
        if (contextCloser != null) {
            contextCloser.close();
        }

        LOGGER.info("TestClass {} FINISHED", context.getRequiredTestClass().getName());
        logVisualSeparator(context);
    }

    @Override
    public void beforeEach(@NonNull ExtensionContext context) {
        TestConfig testConfig = getTestConfig(context);
        if (testConfig == null) {
            return;
        }

        KubeResourceManager resourceManager = getResourceManager(context);
        if (resourceManager != null) {
            resourceManager.setTestContext(context);
        }

        // Inject fields into the current test instance
        injectTestClassFields(context);

        logVisualSeparator(context);
        LOGGER.info("Test {}.{} STARTED", context.getRequiredTestClass().getName(),
            context.getDisplayName().replace("()", ""));
    }

    @Override
    public void afterEach(@NonNull ExtensionContext context) {
        TestConfig testConfig = getTestConfig(context);
        if (testConfig == null) {
            return;
        }

        // Handle cleanup by delegating to ResourceManager logic
        handleAutomaticCleanup(context, testConfig);

        String state = "SUCCEEDED";
        if (context.getExecutionException().isPresent()) {
            state = "FAILED";
        }

        // Collect logs if configured for AFTER_EACH (successful completion)
        if ("SUCCEEDED".equals(state) && testConfig.collectLogs() &&
            testConfig.logCollectionStrategy() == LogCollectionStrategy.AFTER_EACH) {
            String testName = context.getDisplayName().replace("()", "");
            collectLogs(context, "after-each-success-" + testName.toLowerCase());
        }

        LOGGER.info("Test {}.{} {}", context.getRequiredTestClass().getName(),
            context.getDisplayName().replace("()", ""), state);
        logVisualSeparator(context);
    }

    // ===============================
    // Exception Handlers for Comprehensive Log Collection
    // ===============================

    @Override
    public void handleTestExecutionException(ExtensionContext context, @NonNull Throwable throwable)
        throws Throwable {
        LOGGER.error("Test failed during execution: {}", context.getDisplayName(), throwable);
        handleTestFailure(context, "test-execution");
        throw throwable;
    }

    @Override
    public void handleBeforeAllMethodExecutionException(ExtensionContext context, @NonNull Throwable throwable)
        throws Throwable {
        LOGGER.error("Test failed during beforeAll: {}", context.getDisplayName(), throwable);
        handleTestFailure(context, "before-all");
        throw throwable;
    }

    @Override
    public void handleBeforeEachMethodExecutionException(ExtensionContext context, @NonNull Throwable throwable)
        throws Throwable {
        LOGGER.error("Test failed during beforeEach: {}", context.getDisplayName(), throwable);
        handleTestFailure(context, "before-each");
        throw throwable;
    }

    @Override
    public void handleAfterEachMethodExecutionException(ExtensionContext context, @NonNull Throwable throwable)
        throws Throwable {
        LOGGER.error("Test failed during afterEach: {}", context.getDisplayName(), throwable);
        handleTestFailure(context, "after-each");
        throw throwable;
    }

    @Override
    public void handleAfterAllMethodExecutionException(ExtensionContext context, @NonNull Throwable throwable)
        throws Throwable {
        LOGGER.error("Test failed during afterAll: {}", context.getDisplayName(), throwable);
        handleTestFailure(context, "after-all");
        throw throwable;
    }

    private void handleTestFailure(ExtensionContext context, String phase) {
        TestConfig testConfig = getTestConfig(context);
        if (testConfig == null || !testConfig.collectLogs()) {
            return;
        }

        // Collect logs based on strategy
        LogCollectionStrategy strategy = testConfig.logCollectionStrategy();
        if (strategy == LogCollectionStrategy.ON_FAILURE ||
            strategy == LogCollectionStrategy.AFTER_EACH) {

            String testName = context.getDisplayName().replace("()", "");
            String suffix = String.format("failure-%s-%s", phase, testName.toLowerCase());
            collectLogs(context, suffix);
        }

        // Handle cleanup on failure by delegating to ResourceManager logic
        if (testConfig.cleanup() == CleanupStrategy.AUTOMATIC) {
            LOGGER.info("Cleaning up resources due to test failure in phase: {}", phase);
            handleAutomaticCleanup(context, testConfig);
        }
    }

    @Override
    public boolean supportsParameter(@NonNull ParameterContext parameterContext,
                                     @NonNull ExtensionContext extensionContext) throws ParameterResolutionException {
        return hasInjectAnnotation(parameterContext);
    }

    @Override
    public Object resolveParameter(@NonNull ParameterContext parameterContext,
                                   @NonNull ExtensionContext extensionContext) throws ParameterResolutionException {
        return injectParameter(parameterContext, extensionContext);
    }

    private KubernetesTest getKubernetesTestAnnotation(ExtensionContext context) {
        return context.getRequiredTestClass().getAnnotation(KubernetesTest.class);
    }

    private TestConfig createTestConfig(ExtensionContext context, KubernetesTest annotation) {
        String[] namespaces = annotation.namespaces().length == 0 ?
            new String[]{generateNamespace(context)} : annotation.namespaces();

        return new TestConfig(
            namespaces,
            annotation.createNamespaces(),
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

    private void injectTestClassFields(ExtensionContext context) {
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
            parameterContext.isAnnotated(InjectResource.class) ||
            parameterContext.isAnnotated(InjectNamespaces.class) ||
            parameterContext.isAnnotated(InjectNamespace.class);
    }

    private Object injectParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        if (parameterContext.isAnnotated(InjectKubeClient.class)) {
            return injectKubeClient(parameterContext, extensionContext);
        } else if (parameterContext.isAnnotated(InjectCmdKubeClient.class)) {
            return injectCmdKubeClient(parameterContext, extensionContext);
        } else if (parameterContext.isAnnotated(InjectResourceManager.class)) {
            return getResourceManager(extensionContext);
        } else if (parameterContext.isAnnotated(InjectResource.class)) {
            return injectResource(parameterContext, extensionContext);
        } else if (parameterContext.isAnnotated(InjectNamespaces.class)) {
            return injectNamespaces(parameterContext, extensionContext);
        } else if (parameterContext.isAnnotated(InjectNamespace.class)) {
            return injectNamespace(parameterContext, extensionContext);
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
        } else if (field.isAnnotationPresent(InjectResource.class)) {
            return injectResourceForField(field, context);
        } else if (field.isAnnotationPresent(InjectNamespaces.class)) {
            return injectNamespacesForField(field, context);
        } else if (field.isAnnotationPresent(InjectNamespace.class)) {
            return injectNamespaceForField(field, context);
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


    private Object injectResource(ParameterContext parameterContext, ExtensionContext extensionContext) {
        InjectResource annotation = parameterContext.getParameter().getAnnotation(InjectResource.class);
        return loadAndInjectResource(annotation, parameterContext.getParameter().getType(), extensionContext);
    }

    private Object injectResourceForField(Field field, ExtensionContext context) {
        InjectResource annotation = field.getAnnotation(InjectResource.class);
        return loadAndInjectResource(annotation, field.getType(), context);
    }

    private Object injectNamespaces(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return getNamespaceObjects(extensionContext);
    }

    private Object injectNamespacesForField(Field field, ExtensionContext context) {
        return getNamespaceObjects(context);
    }

    private Object injectNamespace(ParameterContext parameterContext, ExtensionContext extensionContext) {
        InjectNamespace annotation = parameterContext.getParameter().getAnnotation(InjectNamespace.class);
        String namespaceName = annotation.name();

        Map<String, Namespace> namespaceObjects = getNamespaceObjects(extensionContext);
        if (namespaceObjects == null) {
            throw new ParameterResolutionException("Namespace objects not available");
        }

        Namespace namespace = namespaceObjects.get(namespaceName);
        if (namespace == null) {
            throw new ParameterResolutionException("Namespace '" + namespaceName + "' not found in test namespaces. " +
                "Make sure it's defined in @KubernetesTest annotation.");
        }

        return namespace;
    }

    private Object injectNamespaceForField(Field field, ExtensionContext context) {
        InjectNamespace annotation = field.getAnnotation(InjectNamespace.class);
        String namespaceName = annotation.name();

        Map<String, Namespace> namespaceObjects = getNamespaceObjects(context);
        if (namespaceObjects == null) {
            throw new RuntimeException("Namespace objects not available");
        }

        Namespace namespace = namespaceObjects.get(namespaceName);
        if (namespace == null) {
            throw new RuntimeException("Namespace '" + namespaceName + "' not found in test namespaces. " +
                "Make sure it's defined in @KubernetesTest annotation.");
        }

        return namespace;
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
                .orElse(resources.getFirst());


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
        return KubeResourceManager.get();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Namespace> getNamespaceObjects(ExtensionContext context) {
        return (Map<String, Namespace>) context.getStore(ExtensionContext.Namespace.GLOBAL)
            .get(NAMESPACE_OBJECTS_KEY);
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
            Paths.get(System.getProperty("user.dir"), "target", "test-logs").toString() :
            testConfig.logCollectionPath();

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
            LOGGER.info("Collecting logs for test execution: {}", suffix);

            // Create label selector to find namespaces with log collection enabled
            LabelSelector logCollectionSelector = new LabelSelectorBuilder()
                .addToMatchLabels(LOG_COLLECTION_LABEL_KEY, LOG_COLLECTION_LABEL_VALUE)
                .build();

            LOGGER.debug("Looking for namespaces with label: {}={}", LOG_COLLECTION_LABEL_KEY,
                LOG_COLLECTION_LABEL_VALUE);

            KubeResourceManager resourceManager = getResourceManager(context);
            List<String> labeledNamespaces = resourceManager.kubeClient().getClient()
                .namespaces()
                .withLabelSelector(logCollectionSelector)
                .list()
                .getItems()
                .stream()
                .map(ns -> ns.getMetadata().getName())
                .toList();

            LOGGER.info("Found {} namespaces with log collection label: {}", labeledNamespaces.size(),
                labeledNamespaces);

            if (labeledNamespaces.isEmpty()) {
                LOGGER.warn("No namespaces found with log collection label - checking all test namespaces");
                // Fallback: collect from all test namespaces
                String[] testNamespaces = testConfig.namespaces();
                LOGGER.info("Fallback: collecting from test namespaces: {}", String.join(", ", testNamespaces));
                logCollector.collectFromNamespaces(testNamespaces);
            } else {
                // Use label selector as intended
                logCollector.collectFromNamespacesWithLabelsToFolder(logCollectionSelector, null);
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

    private void setupNamespaceAutoLabeling(KubeResourceManager resourceManager) {
        LOGGER.info("Setting up automatic namespace labeling for log collection");

        resourceManager.addCreateCallback(resource -> {
            LOGGER.debug("Resource create callback triggered for: {} - {}",
                resource.getKind(), resource.getMetadata().getName());

            if ("Namespace".equals(resource.getKind())) {
                String namespaceName = resource.getMetadata().getName();
                LOGGER.info("Auto-labeling namespace '{}' for log collection", namespaceName);

                try {
                    KubeUtils.labelNamespace(namespaceName, LOG_COLLECTION_LABEL_KEY, LOG_COLLECTION_LABEL_VALUE);
                    LOGGER.info("Successfully labeled namespace '{}' with {}={}",
                        namespaceName, LOG_COLLECTION_LABEL_KEY, LOG_COLLECTION_LABEL_VALUE);
                } catch (Exception e) {
                    LOGGER.error("Failed to label namespace '{}' for log collection: {}",
                        namespaceName, e.getMessage(), e);
                }
            }
        });
    }

    private void setupNamespaces(ExtensionContext context, TestConfig testConfig,
                                 KubeResourceManager resourceManager) {
        String[] namespaceNames = testConfig.namespaces();
        Map<String, Namespace> namespaceObjects = new HashMap<>();
        List<String> createdNamespaces = new ArrayList<>();

        LOGGER.info("Setting up test namespaces: {}", String.join(", ", namespaceNames));

        for (String namespaceName : namespaceNames) {
            // Check if namespace already exists
            Namespace existingNamespace = resourceManager.kubeClient().getClient().namespaces()
                .withName(namespaceName).get();

            if (existingNamespace != null) {
                LOGGER.info("Using existing namespace: {}", namespaceName);
                namespaceObjects.put(namespaceName, existingNamespace);
            } else {
                if (testConfig.createNamespaces()) {
                    LOGGER.info("Creating new namespace: {}", namespaceName);

                    // Start with basic namespace
                    var metadataBuilder = new NamespaceBuilder()
                        .withNewMetadata()
                        .withName(namespaceName);

                    // Add custom labels
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

                    // Get the created namespace from cluster for injection
                    Namespace createdNamespace = resourceManager.kubeClient().getClient().namespaces()
                        .withName(namespaceName).get();
                    namespaceObjects.put(namespaceName, createdNamespace);
                    createdNamespaces.add(namespaceName);
                } else {
                    throw new RuntimeException("Namespace '" + namespaceName +
                        "' does not exist and createNamespaces is false");
                }
            }
        }

        // Store which namespaces we actually created (for cleanup)
        if (!createdNamespaces.isEmpty()) {
            context.getStore(ExtensionContext.Namespace.GLOBAL).put(CREATED_NAMESPACE_KEY, true);
            context.getStore(ExtensionContext.Namespace.GLOBAL).put(CREATED_NAMESPACE_NAMES_KEY, createdNamespaces);
            LOGGER.info("Created namespaces: {}", String.join(", ", createdNamespaces));
        } else {
            LOGGER.info("No new namespaces were created - all namespaces already existed");
        }

        // Store namespace objects for injection
        context.getStore(ExtensionContext.Namespace.GLOBAL).put(NAMESPACE_OBJECTS_KEY, namespaceObjects);
        context.getStore(ExtensionContext.Namespace.GLOBAL).put(NAMESPACE_KEY, namespaceNames);
        LOGGER.info("Using namespaces: {}", String.join(", ", namespaceNames));
    }

    private void cleanupNamespaces(ExtensionContext context, TestConfig testConfig) {
        // Only delete namespaces that were actually created by this test
        @SuppressWarnings("unchecked")
        List<String> createdNamespaces = (List<String>) context.getStore(ExtensionContext.Namespace.GLOBAL)
            .get(CREATED_NAMESPACE_NAMES_KEY);

        if (createdNamespaces == null || createdNamespaces.isEmpty()) {
            LOGGER.info("No namespaces to delete - all namespaces were existing before the test");
            return;
        }

        LOGGER.info("Deleting only test-created namespaces: {}", String.join(", ", createdNamespaces));
        LOGGER.info("Protecting existing namespaces from deletion");

        try {
            KubeResourceManager resourceManager = getResourceManager(context);
            if (resourceManager != null) {
                for (String namespaceName : createdNamespaces) {
                    LOGGER.debug("Deleting test-created namespace: {}", namespaceName);
                    Namespace namespace = new NamespaceBuilder()
                        .withNewMetadata()
                        .withName(namespaceName)
                        .endMetadata()
                        .build();
                    resourceManager.deleteResourceWithWait(namespace);
                }
                LOGGER.info("Successfully deleted {} test-created namespaces", createdNamespaces.size());
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to delete test-created namespaces: {}", String.join(", ", createdNamespaces), e);
        }
    }

    /**
     * Handle automatic cleanup by delegating to the same logic as @ResourceManager.
     * This mimics what ResourceManagerCleanerExtension does but checks our CleanupStrategy instead
     * of looking for @ResourceManager annotation.
     */
    private void handleAutomaticCleanup(ExtensionContext context, TestConfig testConfig) {
        if (testConfig.cleanup() == CleanupStrategy.AUTOMATIC) {
            KubeResourceManager resourceManager = KubeResourceManager.get();
            resourceManager.setTestContext(context);
            resourceManager.deleteResources(true);
        }
    }

}
