/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.clients;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import io.skodjob.testframe.TestFrameConstants;
import io.skodjob.testframe.TestFrameEnv;
import io.skodjob.testframe.executor.Exec;

/**
 * Provides functionality to interact with Kubernetes and OpenShift clusters.
 * This includes creating clients, reading resources from files, and managing kubeconfig for authentication.
 */
public class KubeClient {

    private KubernetesClient client;
    private String kubeconfigPath;

    /**
     * Initializes the Kubernetes client with configuration derived from environment variables or default context.
     */
    public KubeClient() {
        Config config = getConfig();
        this.client = new KubernetesClientBuilder().withConfig(config).build();
    }

    /**
     * Returns the Kubernetes client.
     *
     * @return The initialized Kubernetes client.
     */
    public KubernetesClient getClient() {
        return client;
    }

    /**
     * Test method only
     * Reconnect client with new config
     * @param config kubernetes config
     */
    void testReconnect(Config config) {
        this.client = new KubernetesClientBuilder().withConfig(config).build();
    }

    /**
     * Adapts the Kubernetes client to an OpenShift client.
     *
     * @return An instance of OpenShiftClient.
     */
    public OpenShiftClient getOpenShiftClient() {
        return client.adapt(OpenShiftClient.class);
    }

    /**
     * Returns the path to the kubeconfig file used for authentication.
     *
     * @return The kubeconfig file path.
     */
    public String getKubeconfigPath() {
        return kubeconfigPath;
    }

    /**
     * Reads Kubernetes resources from a file at the specified path.
     *
     * @param file The path to the file containing Kubernetes resources.
     * @return A list of {@link HasMetadata} resources defined in the file.
     * @throws IOException If an I/O error occurs reading from the file.
     */
    public List<HasMetadata> readResourcesFromFile(Path file) throws IOException {
        return readResourcesFromFile(Files.newInputStream(file));
    }

    /**
     * Reads Kubernetes resources from an InputStream.
     *
     * @param is The InputStream containing Kubernetes resources.
     * @return A list of {@link HasMetadata} resources defined in the stream.
     * @throws IOException If an I/O error occurs.
     */
    public List<HasMetadata> readResourcesFromFile(InputStream is) throws IOException {
        try (is) {
            return client.load(is).items();
        }
    }

    /**
     * Determines the appropriate Kubernetes configuration based on environment variables.
     *
     * @return Config The Kubernetes client configuration.
     */
    private Config getConfig() {
        if (TestFrameEnv.KUBE_USERNAME != null && TestFrameEnv.KUBE_PASSWORD != null && TestFrameEnv.KUBE_URL != null) {
            kubeconfigPath = createLocalKubeconfig();
            return new ConfigBuilder()
                    .withUsername(TestFrameEnv.KUBE_USERNAME)
                    .withPassword(TestFrameEnv.KUBE_PASSWORD)
                    .withMasterUrl(TestFrameEnv.KUBE_URL)
                    .withDisableHostnameVerification(true)
                    .withTrustCerts(true)
                    .build();
        } else if (TestFrameEnv.KUBE_URL != null && TestFrameEnv.KUBE_TOKEN != null) {
            kubeconfigPath = createLocalKubeconfig();
            return new ConfigBuilder()
                    .withOauthToken(TestFrameEnv.KUBE_TOKEN)
                    .withMasterUrl(TestFrameEnv.KUBE_URL)
                    .withDisableHostnameVerification(true)
                    .withTrustCerts(true)
                    .build();
        } else {
            return Config.autoConfigure(System.getenv().getOrDefault("KUBE_CONTEXT", null));
        }
    }

    /**
     * Creates a local kubeconfig file based on environment variables and configuration settings.
     *
     * @return The path to the created kubeconfig file or null if creation failed.
     */
    private String createLocalKubeconfig() {
        try {
            if (TestFrameEnv.CLIENT_TYPE.equals(TestFrameConstants.OPENSHIFT_CLIENT)) {
                if (TestFrameEnv.KUBE_URL != null && TestFrameEnv.KUBE_TOKEN != null) {
                    createLocalOcKubeconfig(TestFrameEnv.KUBE_TOKEN, TestFrameEnv.KUBE_URL);
                } else {
                    createLocalOcKubeconfig(
                            TestFrameEnv.KUBE_USERNAME, TestFrameEnv.KUBE_PASSWORD, TestFrameEnv.KUBE_URL);
                }
            } else {
                if (TestFrameEnv.KUBE_URL != null && TestFrameEnv.KUBE_TOKEN != null) {
                    createLocalKubectlContext(TestFrameEnv.KUBE_TOKEN, TestFrameEnv.KUBE_URL);
                } else {
                    createLocalKubectlContext(
                            TestFrameEnv.KUBE_USERNAME, TestFrameEnv.KUBE_PASSWORD, TestFrameEnv.KUBE_URL);
                }
            }
            return TestFrameEnv.USER_PATH + "/test.kubeconfig";
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Configures a local kubeconfig for OpenShift using oc login with username and password.
     *
     * @param username The username for OpenShift login.
     * @param password The password for OpenShift login.
     * @param apiUrl   The URL of the OpenShift cluster API.
     */
    private void createLocalOcKubeconfig(String username, String password, String apiUrl) {
        Exec.exec(null, Arrays.asList("oc", "login",
                "-u", username,
                "-p", password,
                "--insecure-skip-tls-verify",
                "--kubeconfig", TestFrameEnv.USER_PATH + "/test.kubeconfig",
                apiUrl), 0, false, true);
    }

    /**
     * Configures a local kubeconfig for OpenShift using oc login with a token.
     *
     * @param token  The token for OpenShift login.
     * @param apiUrl The URL of the OpenShift cluster API.
     */
    private void createLocalOcKubeconfig(String token, String apiUrl) {
        Exec.exec(null, Arrays.asList("oc", "login", "--token", token,
                "--insecure-skip-tls-verify",
                "--kubeconfig", TestFrameEnv.USER_PATH + "/test.kubeconfig",
                apiUrl), 0, false, true);
    }

    /**
     * Configures a local kubeconfig for Kubernetes using kubectl to set credentials with username and password.
     *
     * @param username The username for Kubernetes cluster.
     * @param password The password for Kubernetes cluster.
     * @param apiUrl   The URL of the Kubernetes cluster API.
     */
    private void createLocalKubectlContext(String username, String password, String apiUrl) {
        Exec.exec(null, Arrays.asList("kubectl", "config",
                        "set-credentials", "test-user",
                        "--username", username,
                        "--password", password,
                        "--kubeconfig", TestFrameEnv.USER_PATH + "/test.kubeconfig"),
                0, false, true);
        buildKubectlContext(apiUrl);
    }

    /**
     * Configures a local kubeconfig for Kubernetes using kubectl to set credentials with a token.
     *
     * @param token  The token for Kubernetes login.
     * @param apiUrl The URL of the Kubernetes cluster API.
     */
    private void createLocalKubectlContext(String token, String apiUrl) {
        Exec.exec(null, Arrays.asList("kubectl", "config",
                        "set-credentials", "test-user",
                        "--token", token,
                        "--kubeconfig", TestFrameEnv.USER_PATH + "/test.kubeconfig"),
                0, false, true);
        buildKubectlContext(apiUrl);
    }

    /**
     * Builds the kubeconfig context for Kubernetes using kubectl, setting up cluster, user, and context information.
     *
     * @param apiUrl The URL of the Kubernetes cluster API.
     */
    private void buildKubectlContext(String apiUrl) {
        Exec.exec(null, Arrays.asList("kubectl", "config",
                        "set-cluster", "test-cluster",
                        "--insecure-skip-tls-verify=true", "--server", apiUrl,
                        "--kubeconfig", TestFrameEnv.USER_PATH + "/test.kubeconfig"),
                0, false, true);
        Exec.exec(null, Arrays.asList("kubectl", "config",
                        "set-context", "test-context",
                        "--user", "test-user",
                        "--cluster", "test-cluster",
                        "--namespace", "default",
                        "--kubeconfig", TestFrameEnv.USER_PATH + "/test.kubeconfig"),
                0, false, true);
        Exec.exec(null, Arrays.asList("kubectl", "config",
                        "use-context", "test-context",
                        "--kubeconfig", TestFrameEnv.USER_PATH + "/test.kubeconfig"),
                0, false, true);
    }
}
