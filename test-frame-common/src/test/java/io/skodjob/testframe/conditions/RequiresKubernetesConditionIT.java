/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.conditions;

import io.skodjob.testframe.annotations.RequiresKubernetes;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Example test demonstrating conditional execution based on Kubernetes cluster availability.
 * Tests annotated with @RequiresKubernetes will only run if a cluster is accessible.
 */
class RequiresKubernetesConditionIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequiresKubernetesConditionIT.class);

    @Test
    void testWithoutKubernetes() {
        // This test always runs regardless of cluster availability
        LOGGER.info("Running test without Kubernetes requirement");
        assertTrue(true, "This test should always run");
    }

    @Test
    @RequiresKubernetes
    void testRequiringKubernetes() {
        // This test only runs if cluster is available
        LOGGER.info("Running test that requires Kubernetes cluster");
        assertTrue(true, "This test only runs when cluster is available");
    }

    @Test
    @RequiresKubernetes(skipReason = "Custom reason: Need active cluster for integration test")
    void testWithCustomSkipReason() {
        LOGGER.info("Running integration test requiring cluster");
        assertTrue(true, "Integration test with custom skip message");
    }

    @Test
    @RequiresKubernetes(timeoutMs = 10000L, checkClusterHealth = true)
    void testWithFullHealthCheck() {
        LOGGER.info("Running test with full cluster health verification");
        assertTrue(true, "Test requiring healthy cluster nodes");
    }

    @Test
    @RequiresKubernetes(context = "nonexistent-context",
                       skipReason = "Test context not available")
    void testWithSpecificContext() {
        LOGGER.info("This test should be skipped due to invalid context");
        assertTrue(true, "This should not run with invalid context");
    }
}

/**
 * Example test class where ALL tests require Kubernetes.
 * Applying @RequiresKubernetes at class level affects all test methods.
 */
@RequiresKubernetes(skipReason = "All tests in this class require Kubernetes cluster")
class KubernetesOnlyIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesOnlyIT.class);

    @Test
    void testOne() {
        LOGGER.info("Test one - requires cluster (inherited from class annotation)");
        assertTrue(true);
    }

    @Test
    void testTwo() {
        LOGGER.info("Test two - also requires cluster");
        assertTrue(true);
    }

    @Test
    void testThree() {
        LOGGER.info("Test three - cluster required as well");
        assertTrue(true);
    }
}