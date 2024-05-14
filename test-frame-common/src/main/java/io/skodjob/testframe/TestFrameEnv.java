/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe;

import io.skodjob.testframe.environment.TestEnvironmentVariables;

/**
 * Class which holds environment variables for system tests.
 */
public final class TestFrameEnv {

    private static final TestEnvironmentVariables ENV_VARIABLES = new TestEnvironmentVariables();

    private static final String CLIENT_TYPE_ENV = "CLIENT_TYPE";
    private static final String TOKEN_ENV = "KUBE_TOKEN";
    private static final String URL_ENV = "KUBE_URL";
    private static final String IP_FAMILY_ENV = "IP_FAMILY";

    /**
     * Represents the default IP family, which is IPv4.
     */
    public static final String IP_FAMILY_DEFAULT = "ipv4";

    /**
     * Represents the IPv6 IP family.
     */
    public static final String IP_FAMILY_VERSION_6 = "ipv6";

    /**
     * Represents the dual stack IP family, which supports both IPv4 and IPv6.
     */
    public static final String IP_FAMILY_DUAL_STACK = "dual";

    /**
     * Default user dir of exec process
     */
    public static final String USER_PATH = System.getProperty("user.dir");

    /**
     * The type of client.
     */
    public static final String CLIENT_TYPE =
        ENV_VARIABLES.getOrDefault(CLIENT_TYPE_ENV, TestFrameConstants.KUBERNETES_CLIENT);

    /**
     * The token for accessing the Kubernetes cluster.
     */
    public static final String KUBE_TOKEN = ENV_VARIABLES.getOrDefault(TOKEN_ENV, null);

    /**
     * The URL for accessing the Kubernetes cluster.
     */
    public static final String KUBE_URL = ENV_VARIABLES.getOrDefault(URL_ENV, null);

    /**
     * The IP address family used for network communication. This can be one of the following:
     * <ul>
     *   <li>ipv4 - IPv4 addressing system.</li>
     *   <li>ipv6 - IPv6 addressing system.</li>
     *   <li>dual - Dual stack (IPv4 and IPv6).</li>
     * </ul>
     * The default setting is 'ipv4' if not specified in the environment variables.
     */
    public static final String IP_FAMILY = ENV_VARIABLES.getOrDefault(IP_FAMILY_ENV, IP_FAMILY_DEFAULT);

    private TestFrameEnv() {
        // Private constructor to prevent instantiation
    }

    static {
        ENV_VARIABLES.logEnvironmentVariables();
    }
}
