package io.skodjob.testframe.clients;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.openshift.client.OpenShiftClient;

public class KubeClient {

    private KubernetesClient client;
    private static KubeClient kubeClient;

    private KubeClient() {
        this.client = new KubernetesClientBuilder().withConfig(Config.autoConfigure(System.getenv().getOrDefault("TEST_CLUSTER_CONTEXT", null))).build();
    }

    public static KubeClient getInstance() {
        if (kubeClient == null) {
            kubeClient = new KubeClient();
        }
        return kubeClient;
    }

    public KubernetesClient getClient() {
        return client;
    }

    public OpenShiftClient getOpenShiftClient() {
        return client.adapt(OpenShiftClient.class);
    }
}