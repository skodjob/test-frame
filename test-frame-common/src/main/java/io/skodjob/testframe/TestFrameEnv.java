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

    private TestFrameEnv() {
        // Private constructor to prevent instantiation
    }

    static {
        ENV_VARIABLES.logEnvironmentVariables();
    }
}
