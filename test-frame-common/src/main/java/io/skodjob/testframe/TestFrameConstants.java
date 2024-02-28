/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe;

import java.time.Duration;

public class TestFrameConstants {
    private TestFrameConstants() {
    }
    public static final long GLOBAL_POLL_INTERVAL_LONG = Duration.ofSeconds(15).toMillis();
    public static final long GLOBAL_POLL_INTERVAL_MEDIUM = Duration.ofSeconds(10).toMillis();
    public static final long GLOBAL_POLL_INTERVAL_SHORT = Duration.ofSeconds(5).toMillis();
    public static final long GLOBAL_POLL_INTERVAL_1_SEC = Duration.ofSeconds(1).toMillis();
    public static final long GLOBAL_TIMEOUT = Duration.ofMinutes(10).toMillis();
    public static final String OPENSHIFT_CLIENT = "oc";
    public static final String KUBERNETES_CLIENT = "kubectl";

}
