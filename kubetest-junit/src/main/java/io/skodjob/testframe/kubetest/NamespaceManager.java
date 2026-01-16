/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.kubetest;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.skodjob.testframe.TestFrameConstants;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.utils.KubeUtils;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages namespace lifecycle operations for Kubernetes tests.
 * This class handles namespace creation, setup, cleanup, and auto-labeling
 * for both single-context and multi-context test scenarios.
 */
class NamespaceManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(NamespaceManager.class);

    // Log collection labels
    private static final String LOG_COLLECTION_LABEL_KEY = "test-frame.io/log-collection";
    private static final String LOG_COLLECTION_LABEL_VALUE = "enabled";

    private final ContextStoreHelper contextStoreHelper;
    private final MultiContextProvider multiContextProvider;

    /**
     * Creates a new NamespaceManager with the given dependencies.
     *
     * @param contextStoreHelper   provides access to extension context storage
     * @param multiContextProvider provides multi-context operations
     */
    NamespaceManager(ContextStoreHelper contextStoreHelper, MultiContextProvider multiContextProvider) {
        this.contextStoreHelper = contextStoreHelper;
        this.multiContextProvider = multiContextProvider;
    }

    // ===============================
    // Namespace Setup Methods
    // ===============================

    /**
     * Sets up all namespaces for the test, including multi-context namespaces.
     */
    public void setupNamespaces(ExtensionContext context, TestConfig testConfig,
                                KubeResourceManager resourceManager) {
        List<String> namespaceNames = testConfig.namespaces();
        Map<String, Namespace> namespaceObjects = new HashMap<>();
        List<String> createdNamespaces = new ArrayList<>();

        LOGGER.info("Setting up test namespaces: {}", String.join(", ", namespaceNames));

        // Set up primary context namespaces
        for (String namespaceName : namespaceNames) {
            NamespaceSetupContext setupContext = new NamespaceSetupContext(
                namespaceName, "", testConfig, resourceManager,
                namespaceObjects, createdNamespaces, context, null
            );
            performNamespaceSetup(setupContext);
        }

        // Set up context-specific namespaces
        for (TestConfig.ContextMappingConfig contextMapping : testConfig.contextMappings()) {
            String clusterContext = contextMapping.context();
            LOGGER.info("Setting up namespaces for context '{}': {}", clusterContext,
                String.join(", ", contextMapping.namespaces()));

            // Get or create ResourceManager for this context
            KubeResourceManager contextResourceManager =
                multiContextProvider.getResourceManagerForContext(context, clusterContext);
            Map<String, Namespace> contextNamespaceObjects =
                multiContextProvider.getOrCreateNamespaceObjectsForContext(context, clusterContext);
            List<String> contextCreatedNamespaces =
                multiContextProvider.getOrCreateCreatedNamespacesForContext(context, clusterContext);

            for (String namespaceName : contextMapping.namespaces()) {
                NamespaceSetupContext setupContext = new NamespaceSetupContext(
                    namespaceName, clusterContext, testConfig, contextResourceManager,
                    contextNamespaceObjects, contextCreatedNamespaces, context, contextMapping
                );
                performNamespaceSetup(setupContext);
            }

            // Store context-specific namespace objects
            multiContextProvider.storeNamespaceObjectsForContext(context, clusterContext, contextNamespaceObjects);
        }

        // Store which namespaces we actually created (for cleanup)
        if (!createdNamespaces.isEmpty()) {
            contextStoreHelper.putCreatedNamespaceFlag(context, true);
            contextStoreHelper.putCreatedNamespaceNames(context, createdNamespaces);
            LOGGER.info("Created namespaces: {}", String.join(", ", createdNamespaces));
        } else {
            LOGGER.info("No new namespaces were created - all namespaces already existed");
        }

        // Store namespace objects for injection
        contextStoreHelper.putNamespaceObjects(context, namespaceObjects);
        contextStoreHelper.putNamespaces(context, namespaceNames.toArray(new String[0]));
        LOGGER.info("Using namespaces: {}", String.join(", ", namespaceNames));
    }

    /**
     * Sets up automatic namespace labeling for log collection.
     */
    public void setupNamespaceAutoLabeling(KubeResourceManager resourceManager) {
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

    /**
     * Cleans up namespaces that were created during the test.
     */
    public void cleanupNamespaces(ExtensionContext context, TestConfig testConfig) {
        // Only delete namespaces that were actually created by this test
        List<String> createdNamespaces = contextStoreHelper.getCreatedNamespaceNames(context);

        if (createdNamespaces == null || createdNamespaces.isEmpty()) {
            LOGGER.info("No namespaces to delete - all namespaces were existing before the test");
            return;
        }

        LOGGER.info("Deleting only test-created namespaces: {}", String.join(", ", createdNamespaces));

        try {
            KubeResourceManager resourceManager = contextStoreHelper.getResourceManager(context);
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
        } catch (Exception e) {
            LOGGER.warn("Failed to delete test-created namespaces: {}", String.join(", ", createdNamespaces), e);
        }
    }

    // ===============================
    // Private Helper Methods
    // ===============================


    private void performNamespaceSetup(NamespaceSetupContext setupContext) {
        String contextLabel = setupContext.clusterContext().isEmpty() ?
            TestFrameConstants.DEFAULT_CONTEXT_NAME : setupContext.clusterContext();
        LOGGER.debug("Setting up namespace '{}' in context '{}'", setupContext.namespaceName(), contextLabel);

        // Check if namespace already exists
        Namespace existingNamespace = setupContext.resourceManager().kubeClient().getClient().namespaces()
            .withName(setupContext.namespaceName()).get();

        if (existingNamespace != null) {
            LOGGER.info("Using existing namespace '{}' in context '{}'", setupContext.namespaceName(), contextLabel);

            // Do not modify existing namespaces - respect namespace protection principle
            setupContext.namespaceObjects().put(setupContext.namespaceName(), existingNamespace);
        } else {
            boolean shouldCreate = setupContext.contextMapping() != null ?
                setupContext.contextMapping().createNamespaces() : setupContext.testConfig().createNamespaces();
            if (shouldCreate) {
                LOGGER.info("Creating new namespace '{}' in context '{}'", setupContext.namespaceName(), contextLabel);

                // Prepare labels map
                Map<String, String> labels = new HashMap<>();

                // Add custom labels from global config
                for (String label : setupContext.testConfig().namespaceLabels()) {
                    String[] parts = label.split("=", 2);
                    if (parts.length == 2) {
                        labels.put(parts[0], parts[1]);
                    }
                }

                // Add custom labels from context mapping if available
                if (setupContext.contextMapping() != null) {
                    for (String label : setupContext.contextMapping().namespaceLabels()) {
                        String[] parts = label.split("=", 2);
                        if (parts.length == 2) {
                            labels.put(parts[0], parts[1]);
                        }
                    }
                }

                // Prepare annotations map
                Map<String, String> annotations = new HashMap<>();

                // Add annotations from global config
                for (String annotation : setupContext.testConfig().namespaceAnnotations()) {
                    String[] parts = annotation.split("=", 2);
                    if (parts.length == 2) {
                        annotations.put(parts[0], parts[1]);
                    }
                }

                // Add annotations from context mapping if available
                if (setupContext.contextMapping() != null) {
                    for (String annotation : setupContext.contextMapping().namespaceAnnotations()) {
                        String[] parts = annotation.split("=", 2);
                        if (parts.length == 2) {
                            annotations.put(parts[0], parts[1]);
                        }
                    }
                }

                // Build namespace with all labels and annotations
                NamespaceBuilder metadataBuilder = new NamespaceBuilder()
                    .withNewMetadata()
                    .withName(setupContext.namespaceName())
                    .withLabels(labels)
                    .withAnnotations(annotations)
                    .endMetadata();

                Namespace namespace = metadataBuilder.build();
                LOGGER.debug("Creating namespace with labels: {}", labels);
                LOGGER.debug("Creating namespace with annotations: {}", annotations);
                setupContext.resourceManager().createResourceWithWait(namespace);

                // Wait a moment for namespace to be fully synced before retrieving
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // Retrieve the actual namespace from cluster to get complete object with status
                Namespace actualNamespace = setupContext.resourceManager().kubeClient().getClient()
                    .namespaces()
                    .withName(setupContext.namespaceName())
                    .get();

                LOGGER.debug("Retrieved namespace labels: {}",
                    actualNamespace.getMetadata().getLabels());
                LOGGER.debug("Retrieved namespace annotations: {}",
                    actualNamespace.getMetadata().getAnnotations());

                // If labels are missing from retrieved namespace, use the built one but copy status
                if (actualNamespace.getMetadata().getLabels() == null ||
                    actualNamespace.getMetadata().getLabels().size() < labels.size()) {
                    LOGGER.warn("Labels missing from retrieved namespace, using built namespace with status");
                    namespace.setStatus(actualNamespace.getStatus());
                    setupContext.namespaceObjects().put(setupContext.namespaceName(), namespace);
                } else {
                    setupContext.namespaceObjects().put(setupContext.namespaceName(), actualNamespace);
                }
                setupContext.createdNamespaces().add(setupContext.namespaceName());
            } else {
                throw new RuntimeException("Namespace '" + setupContext.namespaceName() +
                    "' does not exist in context '" + contextLabel + "' and createNamespaces is false");
            }
        }
    }

    // ===============================
    // Helper Classes and Interfaces
    // ===============================

    /**
     * Helper record to group namespace setup parameters and reduce method parameter count.
     */
    private record NamespaceSetupContext(
        String namespaceName,
        String clusterContext,
        TestConfig testConfig,
        KubeResourceManager resourceManager,
        Map<String, Namespace> namespaceObjects,
        List<String> createdNamespaces,
        ExtensionContext extensionContext,
        TestConfig.ContextMappingConfig contextMapping
    ) {
    }

    /**
     * Interface to abstract multi-context operations for dependency injection.
     * This allows NamespaceManager to work with multi-context functionality
     * without directly depending on specific implementation details.
     */
    public interface MultiContextProvider {
        /**
         * Gets or creates a KubeResourceManager for the specified context.
         *
         * @param context        the extension context
         * @param clusterContext the cluster context name
         * @return the resource manager for the specified context
         */
        KubeResourceManager getResourceManagerForContext(ExtensionContext context, String clusterContext);

        /**
         * Gets or creates namespace objects for the specified context.
         *
         * @param context        the extension context
         * @param clusterContext the cluster context name
         * @return map of namespace names to namespace objects
         */
        Map<String, Namespace> getOrCreateNamespaceObjectsForContext(ExtensionContext context, String clusterContext);

        /**
         * Gets or creates the list of created namespaces for the specified context.
         *
         * @param context        the extension context
         * @param clusterContext the cluster context name
         * @return list of created namespace names
         */
        List<String> getOrCreateCreatedNamespacesForContext(ExtensionContext context, String clusterContext);

        /**
         * Stores namespace objects for the specified context.
         *
         * @param context          the extension context
         * @param clusterContext   the cluster context name
         * @param namespaceObjects the namespace objects to store
         */
        void storeNamespaceObjectsForContext(ExtensionContext context, String clusterContext,
                                             Map<String, Namespace> namespaceObjects);
    }
}