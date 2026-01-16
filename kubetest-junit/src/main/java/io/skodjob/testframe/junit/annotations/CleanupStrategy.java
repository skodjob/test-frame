/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.junit.annotations;

/**
 * Strategy for cleaning up Kubernetes resources after tests.
 */
public enum CleanupStrategy {
    /**
     * Clean up resources after each test method.
     */
    AFTER_EACH,

    /**
     * Clean up resources after all test methods in the class.
     */
    AFTER_ALL,

    /**
     * Clean up resources only if the test fails.
     */
    ON_FAILURE,

    /**
     * Never clean up resources automatically.
     * Resources must be cleaned up manually.
     */
    MANUAL
}