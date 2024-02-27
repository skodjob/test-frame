/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.clients;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import io.skodjob.testframe.TestFrameEnv;
import io.skodjob.testframe.executor.Exec;

import java.util.Arrays;

public class KubeClient {

    private KubernetesClient client;
    private String kubeconfigPath;

    public KubeClient() {
        Config config = getConfig();

        this.client = new KubernetesClientBuilder()
                .withConfig(config)
                .build();
    }

    public KubernetesClient getClient() {
        return client;
    }

    public OpenShiftClient getOpenShiftClient() {
        return client.adapt(OpenShiftClient.class);
    }

    public String getKubeconfigPath() {
        return kubeconfigPath;
    }

    private Config getConfig() {
        if (TestFrameEnv.KUBE_USERNAME != null
                && TestFrameEnv.KUBE_PASSWORD != null
                && TestFrameEnv.KUBE_URL != null) {
            kubeconfigPath = createLocalKubeconfig(TestFrameEnv.KUBE_USERNAME, TestFrameEnv.KUBE_PASSWORD, TestFrameEnv.KUBE_URL);
            return new ConfigBuilder()
                    .withUsername(TestFrameEnv.KUBE_USERNAME)
                    .withPassword(TestFrameEnv.KUBE_PASSWORD)
                    .withMasterUrl(TestFrameEnv.KUBE_URL)
                    .withDisableHostnameVerification(true)
                    .withTrustCerts(true)
                    .build();
        } else if (TestFrameEnv.KUBE_URL != null
                && TestFrameEnv.KUBE_TOKEN != null) {
            kubeconfigPath = createLocalKubeconfig(TestFrameEnv.KUBE_TOKEN, TestFrameEnv.KUBE_URL);
            return new ConfigBuilder()
                    .withOauthToken(TestFrameEnv.KUBE_TOKEN)
                    .withMasterUrl(TestFrameEnv.KUBE_URL)
                    .withDisableHostnameVerification(true)
                    .withTrustCerts(true)
                    .build();
        } else {
            return Config.autoConfigure(System.getenv()
                    .getOrDefault("KUBE_CONTEXT", null));
        }
    }

    private String createLocalKubeconfig(String username, String password, String apiUrl) {
        try {
            Exec.exec(null, Arrays.asList("oc", "login", "-u", username,
                    "-p", password,
                    "--insecure-skip-tls-verify",
                    "--kubeconfig", TestFrameEnv.USER_PATH + "/test.kubeconfig",
                    apiUrl), 0, false, true);
            return TestFrameEnv.USER_PATH + "/test.kubeconfig";
        } catch (Exception ex) {
            return null;
        }
    }

    private String createLocalKubeconfig(String token, String apiUrl) {
        try {
            Exec.exec(null, Arrays.asList("oc", "login", "--token", token,
                    "--insecure-skip-tls-verify",
                    "--kubeconfig", TestFrameEnv.USER_PATH + "/test.kubeconfig",
                    apiUrl), 0, false, true);
            return TestFrameEnv.USER_PATH + "/test.kubeconfig";
        } catch (Exception ex) {
            return null;
        }
    }
}