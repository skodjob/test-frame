/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.kubetest;

import io.skodjob.testframe.kubetest.annotations.CleanupStrategy;
import io.skodjob.testframe.kubetest.annotations.LogCollectionStrategy;

/**
 * Configuration holder for test setup.
 *
 * @param namespaces                  The Kubernetes namespaces to create for testing
 * @param createNamespaces            Whether to create the namespaces if they don't exist
 * @param cleanup                     The cleanup strategy for resources
 * @param context                     The Kubernetes cluster context to use
 * @param storeYaml                   Whether to store YAML representations of resources
 * @param yamlStorePath               Directory path to store YAML files
 * @param namespaceLabels             Labels to apply to the test namespaces
 * @param namespaceAnnotations        Annotations to apply to the test namespaces
 * @param visualSeparatorChar         Character to use for visual separators
 * @param visualSeparatorLength       Length of visual separator lines
 * @param collectLogs                 Whether log collection is enabled
 * @param logCollectionStrategy       When to collect logs
 * @param logCollectionPath           Directory path for log collection
 * @param collectPreviousLogs         Whether to collect previous container logs
 * @param collectNamespacedResources  Namespaced resource types to collect
 * @param collectClusterWideResources Cluster-wide resource types to collect
 * @param collectEvents               Whether to collect events
 */
public record TestConfig(
    String[] namespaces,
    boolean createNamespaces,
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
) {
}