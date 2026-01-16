/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.junit.annotations;

/**
 * Enumeration defining when log collection should occur during test execution.
 */
public enum LogCollectionStrategy {

    /**
     * Collect logs only when a test fails.
     * This is the most common strategy for debugging test failures.
     */
    ON_FAILURE,

    /**
     * Collect logs after each test method completes, regardless of success/failure.
     * Useful for comprehensive debugging and test analysis.
     */
    AFTER_EACH,

    /**
     * Collect logs after all tests in the class complete.
     * Good for performance when you only need final state analysis.
     */
    AFTER_ALL,

    /**
     * Collect logs both before and after test execution.
     * Provides complete state capture for complex debugging scenarios.
     */
    BEFORE_AND_AFTER,

    /**
     * Never collect logs automatically.
     * Log collection can still be triggered manually via the injected LogCollector.
     */
    NEVER
}