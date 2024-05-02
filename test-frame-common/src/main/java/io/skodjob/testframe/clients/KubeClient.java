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
import java.util.function.Function;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import io.skodjob.testframe.LoggerUtils;
import io.skodjob.testframe.TestFrameConstants;
import io.skodjob.testframe.TestFrameEnv;
import io.skodjob.testframe.executor.Exec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides functionality to interact with Kubernetes and OpenShift clusters.
 * This includes creating clients, reading resources from files, and managing kubeconfig for authentication.
 */
public class KubeClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(KubeClient.class);


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
     * Check if namespace exists in current cluster
     * @param namespace namespace name
     * @return true if namespace exists
     */
    public boolean namespaceExists(String namespace) {
        return client.namespaces().list().getItems().stream().map(n -> n.getMetadata().getName())
                .toList().contains(namespace);
    }

    /**
     * Creates resource and apply modifier
     * @param resources resources
     * @param modifier modifier
     */
    public void create(List<HasMetadata> resources, Function<HasMetadata, HasMetadata> modifier) {
        create(null, resources, modifier);
    }

    /**
     * Updates resources and apply modifier
     * @param resources resources
     * @param modifier modifier
     */
    public void update(List<HasMetadata> resources, Function<HasMetadata, HasMetadata> modifier) {
        update(null, resources, modifier);
    }

    /**
     * Creates resource and apply modifier
     * @param namespace namespace
     * @param resources resources
     * @param modifier modifier
     */
    public void create(String namespace, List<HasMetadata> resources, Function<HasMetadata, HasMetadata> modifier) {
        resources.forEach(res -> {
            HasMetadata h = modifier.apply(res);
            LOGGER.debug(LoggerUtils.RESOURCE_WITH_NAMESPACE_LOGGER_PATTERN,
                    "Creating", h.getKind(), h.getMetadata().getName(), namespace);
            if (namespace == null) {
                client.resource(h).create();
            } else {
                client.resource(h).inNamespace(namespace).create();
            }
        });
    }

    /**
     * Updates resources and apply modifier
     * @param namespace namespace
     * @param resources resources
     * @param modifier modifier
     */
    public void update(String namespace, List<HasMetadata> resources, Function<HasMetadata, HasMetadata> modifier) {
        resources.forEach(res -> {
            HasMetadata h = modifier.apply(res);
            LOGGER.debug(LoggerUtils.RESOURCE_WITH_NAMESPACE_LOGGER_PATTERN,
                    "Updating", h.getKind(), h.getMetadata().getName(), namespace);
            if (namespace == null) {
                client.resource(h).update();
            } else {
                client.resource(h).inNamespace(namespace).update();
            }
        });
    }

    /**
     * Create or update resources from file and apply modifier
     * @param resources resources
     * @param modifier modifier method
     */
    public void createOrUpdate(List<HasMetadata> resources, Function<HasMetadata, HasMetadata> modifier) {
        createOrUpdate(null, resources, modifier);
    }

    /**
     * Create or update resources from file and apply modifier
     * @param ns namespace
     * @param resources resources
     * @param modifier modifier method
     */
    public void createOrUpdate(String ns, List<HasMetadata> resources, Function<HasMetadata, HasMetadata> modifier) {
        resources.forEach(i -> {
            HasMetadata h = modifier.apply(i);
            if (h != null) {
                if (client.resource(h).get() == null) {
                    LOGGER.debug(LoggerUtils.RESOURCE_WITH_NAMESPACE_LOGGER_PATTERN,
                            "Creating", h.getKind(), h.getMetadata().getName(), h.getMetadata().getNamespace());
                    if (ns == null) {
                        client.resource(h).create();
                    } else {
                        client.resource(h).inNamespace(ns).create();
                    }
                } else {
                    LOGGER.debug(LoggerUtils.RESOURCE_WITH_NAMESPACE_LOGGER_PATTERN,
                            "Updating", h.getKind(), h.getMetadata().getName(), h.getMetadata().getNamespace());
                    if (ns == null) {
                        client.resource(h).update();
                    } else {
                        client.resource(h).inNamespace(ns).update();
                    }
                }
            }
        });
    }

    /**
     * Deletes resoruces
     * @param resources resources
     */
    public void delete(List<HasMetadata> resources) {
        resources.forEach(h -> {
            if (h != null) {
                if (client.resource(h).get() != null) {
                    LOGGER.debug(LoggerUtils.RESOURCE_WITH_NAMESPACE_LOGGER_PATTERN,
                            "Deleting", h.getKind(), h.getMetadata().getName(), h.getMetadata().getNamespace());
                    client.resource(h).delete();
                }
            }
        });
    }

    /**
     * Delete resources
     * @param resources resources
     * @param namespace namespace
     */
    public void delete(List<HasMetadata> resources, String namespace) {
        resources.forEach(h -> {
            if (h != null) {
                if (client.resource(h).inNamespace(namespace).get() != null) {
                    LOGGER.debug(LoggerUtils.RESOURCE_WITH_NAMESPACE_LOGGER_PATTERN,
                            "Deleting", h.getKind(), h.getMetadata().getName(), namespace);
                    client.resource(h).inNamespace(namespace).delete();
                }
            }
        });
    }

    /**
     * Return log from pod with one container
     * @param namespaceName namespace of the pod
     * @param podName pod name
     * @return logs
     */
    public String getLogsFromPod(String namespaceName, String podName) {
        return client.pods().inNamespace(namespaceName).withName(podName).getLog();
    }

    /**
     * Return log from pods specific container
     * @param namespaceName namespace of the pod
     * @param podName pod name
     * @param containerName container name
     * @return logs
     */
    public String getLogsFromContainer(String namespaceName, String podName, String containerName) {
        return client.pods().inNamespace(namespaceName).withName(podName).inContainer(containerName).getLog();
    }

    /**
     * Returns list of deployments with prefix name
     * @param namespace namespace
     * @param namePrefix prefix
     * @return list of deployments
     */
    public String getDeploymentNameByPrefix(String namespace, String namePrefix) {
        List<Deployment> prefixDeployments = client.apps().deployments()
                .inNamespace(namespace).list().getItems().stream().filter(rs ->
                        rs.getMetadata().getName().startsWith(namePrefix)).toList();

        if (!prefixDeployments.isEmpty()) {
            return prefixDeployments.get(0).getMetadata().getName();
        } else {
            return null;
        }
    }

    /**
     * Determines the appropriate Kubernetes configuration based on environment variables.
     *
     * @return Config The Kubernetes client configuration.
     */
    private Config getConfig() {
        if (TestFrameEnv.KUBE_URL != null && TestFrameEnv.KUBE_TOKEN != null) {
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
                createLocalOcKubeconfig(TestFrameEnv.KUBE_TOKEN, TestFrameEnv.KUBE_URL);
            } else {
                createLocalKubectlContext(TestFrameEnv.KUBE_TOKEN, TestFrameEnv.KUBE_URL);
            }
            return TestFrameEnv.USER_PATH + "/test.kubeconfig";
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
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
