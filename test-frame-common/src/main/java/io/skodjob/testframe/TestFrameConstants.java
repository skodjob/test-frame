/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe;

import java.time.Duration;

/**
 * Constants used in the test framework.
 */
public class TestFrameConstants {
    private TestFrameConstants() {
        // Private constructor to prevent instantiation
    }

    /**
     * Global poll interval in milliseconds (long).
     */
    public static final long GLOBAL_POLL_INTERVAL_LONG = Duration.ofSeconds(15).toMillis();

    /**
     * Global poll interval in milliseconds (medium).
     */
    public static final long GLOBAL_POLL_INTERVAL_MEDIUM = Duration.ofSeconds(10).toMillis();

    /**
     * Global poll interval in milliseconds (short).
     */
    public static final long GLOBAL_POLL_INTERVAL_SHORT = Duration.ofSeconds(5).toMillis();

    /**
     * Global poll interval in milliseconds (1 second).
     */
    public static final long GLOBAL_POLL_INTERVAL_1_SEC = Duration.ofSeconds(1).toMillis();

    /**
     * Global timeout in milliseconds.
     */
    public static final long GLOBAL_TIMEOUT = Duration.ofMinutes(10).toMillis();

    /**
     * OpenShift client type.
     */
    public static final String OPENSHIFT_CLIENT = "oc";

    /**
     * Kubernetes client type.
     */
    public static final String KUBERNETES_CLIENT = "kubectl";
}
