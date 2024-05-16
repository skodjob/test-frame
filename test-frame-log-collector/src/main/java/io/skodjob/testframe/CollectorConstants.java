/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe;

/**
 * Class containing constants used in {@link LogCollector}
 */
public interface CollectorConstants {
    /**
     * Pod resource type
     */
    String POD = "pod";
    /**
     * Container resource type
     */
    String CONTAINER = "container";
    /**
     * Events resource type
     */
    String EVENTS = "events";
    /**
     * Prefix for logs files
     */
    String LOGS = "logs";
    /**
     * Prefix for description files
     */
    String DESCRIBE = "describe";
}
