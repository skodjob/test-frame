/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.kubetest;

import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.skodjob.testframe.LogCollector;
import io.skodjob.testframe.LogCollectorBuilder;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages log collection operations for Kubernetes tests.
 * This class handles log collection setup, multi-context log gathering,
 * and coordination with LogCollector instances across different cluster contexts.
 */
class LogCollectionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogCollectionManager.class);

    // Log collection labels
    private static final String LOG_COLLECTION_LABEL_KEY = "test-frame.io/log-collection";
    private static final String LOG_COLLECTION_LABEL_VALUE = "enabled";

    private final ContextStoreHelper contextStoreHelper;
    private final ConfigurationManager configurationManager;
    private final ContextProvider contextProvider;

    /**
     * Creates a new LogCollectionManager with the given dependencies.
     *
     * @param contextStoreHelper provides access to extension context storage
     * @param configurationManager provides access to test configuration
     * @param contextProvider provides multi-context operations
     */
    LogCollectionManager(ContextStoreHelper contextStoreHelper,
                                ConfigurationManager configurationManager,
                                ContextProvider contextProvider) {
        this.contextStoreHelper = contextStoreHelper;
        this.configurationManager = configurationManager;
        this.contextProvider = contextProvider;
    }

    // ===============================
    // Log Collection Setup
    // ===============================

    /**
     * Sets up the primary LogCollector for the test.
     */
    public void setupLogCollector(ExtensionContext context, TestConfig testConfig,
                                  KubeResourceManager resourceManager) {
        LOGGER.info("Setting up log collector with strategy: {}", testConfig.logCollectionStrategy());

        String logPath = testConfig.logCollectionPath().isEmpty() ?
            Paths.get(System.getProperty("user.dir"), "target", "test-logs").toString() :
            testConfig.logCollectionPath();

        LogCollectorBuilder builder = new LogCollectorBuilder()
            .withRootFolderPath(logPath)
            .withKubeClient(resourceManager.kubeClient())
            .withKubeCmdClient(resourceManager.kubeCmdClient())  // Use context-aware KubeCmdClient
            .withNamespacedResources(testConfig.collectNamespacedResources());

        if (testConfig.collectClusterWideResources().length > 0) {
            builder.withClusterWideResources(testConfig.collectClusterWideResources());
        }

        if (testConfig.collectPreviousLogs()) {
            builder.withCollectPreviousLogs();
        }

        LogCollector logCollector = builder.build();
        contextStoreHelper.putLogCollector(context, logCollector);

        LOGGER.info("Log collector configured to collect to: {}", logPath);
    }

    // ===============================
    // Log Collection Execution
    // ===============================

    /**
     * Collects logs from all contexts with the specified suffix.
     */
    public void collectLogs(ExtensionContext context, String suffix) {
        TestConfig testConfig = configurationManager.getTestConfig(context);
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

            LOGGER.debug("Collecting from namespaces with label: {}={}", LOG_COLLECTION_LABEL_KEY,
                LOG_COLLECTION_LABEL_VALUE);

            // Collect logs from all contexts (primary + context mappings)
            collectLogsFromAllContexts(context, testConfig, logCollectionSelector, logCollector);

            LOGGER.info("Log collection completed successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to collect logs", e);
        }
    }

    /**
     * Collects logs from primary context and all additional contexts.
     */
    private void collectLogsFromAllContexts(ExtensionContext context, TestConfig testConfig,
                                            LabelSelector logCollectionSelector, LogCollector primaryLogCollector) {
        // Collect from primary context
        KubeResourceManager primaryResourceManager = contextProvider.getResourceManager(context);
        List<String> primaryContextNamespaces = collectNamespacesWithLabel(
            primaryResourceManager, logCollectionSelector, "primary");

        // Add primary context test namespaces (includes existing ones that aren't labeled)
        Set<String> primaryContextTestNamespaces = new HashSet<>(Arrays.asList(testConfig.namespaces()));
        primaryContextTestNamespaces.addAll(primaryContextNamespaces);

        if (!primaryContextTestNamespaces.isEmpty()) {
            LOGGER.info("Collecting logs from primary context namespaces: {}", primaryContextTestNamespaces);
            primaryLogCollector.collectFromNamespaces(primaryContextTestNamespaces.toArray(new String[0]));
        }

        // Collect from each additional context using context-specific LogCollectors
        Map<String, KubeResourceManager> contextManagers = contextProvider.getContextManagers(context);
        for (Map.Entry<String, KubeResourceManager> entry : contextManagers.entrySet()) {
            String contextName = entry.getKey();
            KubeResourceManager contextManager = entry.getValue();

            // Find labeled namespaces in this context
            List<String> contextLabeledNamespaces = collectNamespacesWithLabel(
                contextManager, logCollectionSelector, contextName);

            // Find test namespaces for this context
            Set<String> contextTestNamespaces = new HashSet<>();
            for (TestConfig.ContextMappingConfig contextMapping : testConfig.contextMappings()) {
                if (contextName.equals(contextMapping.context())) {
                    contextTestNamespaces.addAll(Arrays.asList(contextMapping.namespaces()));
                }
            }

            // Combine labeled and test namespaces for this context
            contextTestNamespaces.addAll(contextLabeledNamespaces);

            if (!contextTestNamespaces.isEmpty()) {
                LOGGER.info("Collecting logs from context '{}' namespaces: {}", contextName, contextTestNamespaces);

                // Create context-specific LogCollector with proper KubeClient
                LogCollector contextLogCollector =
                    createLogCollectorForContext(testConfig, contextManager, contextName);
                contextLogCollector.collectFromNamespaces(contextTestNamespaces.toArray(new String[0]));
            }
        }

        LOGGER.info("Multi-context log collection completed successfully");
    }

    /**
     * Creates a context-specific LogCollector configured with the appropriate KubeClient
     * for the given context. This ensures log collection uses the correct kubeconfig.
     */
    private LogCollector createLogCollectorForContext(TestConfig testConfig,
                                                      KubeResourceManager contextManager,
                                                      String contextName) {
        String logPath = testConfig.logCollectionPath().isEmpty() ?
            Paths.get(System.getProperty("user.dir"), "target", "test-logs", contextName).toString() :
            Paths.get(testConfig.logCollectionPath(), contextName).toString();

        LOGGER.debug("Creating LogCollector for context '{}' with path: {}", contextName, logPath);

        LogCollectorBuilder builder = new LogCollectorBuilder()
            .withRootFolderPath(logPath)
            .withKubeClient(contextManager.kubeClient())  // Use context-specific KubeClient
            .withKubeCmdClient(contextManager.kubeCmdClient())  // Use context-specific KubeCmdClient
            .withNamespacedResources(testConfig.collectNamespacedResources());

        if (testConfig.collectClusterWideResources().length > 0) {
            builder.withClusterWideResources(testConfig.collectClusterWideResources());
        }

        if (testConfig.collectPreviousLogs()) {
            builder.withCollectPreviousLogs();
        }

        return builder.build();
    }

    /**
     * Collects namespaces that have the log collection label in the specified context.
     */
    private List<String> collectNamespacesWithLabel(KubeResourceManager resourceManager,
                                                    LabelSelector logCollectionSelector, String contextName) {
        try {
            List<String> labeledNamespaces = resourceManager.kubeClient().getClient()
                .namespaces()
                .withLabelSelector(logCollectionSelector)
                .list()
                .getItems()
                .stream()
                .map(ns -> ns.getMetadata().getName())
                .toList();

            LOGGER.debug("Found {} namespaces with log collection label in context '{}': {}",
                labeledNamespaces.size(), contextName, labeledNamespaces);

            return labeledNamespaces;
        } catch (Exception e) {
            LOGGER.warn("Failed to query namespaces in context '{}': {}", contextName, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Gets the stored LogCollector from the extension context.
     */
    private LogCollector getLogCollector(ExtensionContext context) {
        return contextStoreHelper.getLogCollector(context);
    }

    // ===============================
    // Context Provider Interface
    // ===============================

    /**
     * Interface to abstract context operations for dependency injection.
     * This allows LogCollectionManager to work with multi-context functionality
     * without directly depending on specific implementation details.
     */
    public interface ContextProvider {
        /**
         * Gets the primary resource manager for the context.
         *
         * @param context the extension context
         * @return the primary resource manager
         */
        KubeResourceManager getResourceManager(ExtensionContext context);

        /**
         * Gets all context managers for multi-context operations.
         *
         * @param context the extension context
         * @return map of context names to resource managers
         */
        Map<String, KubeResourceManager> getContextManagers(ExtensionContext context);
    }
}