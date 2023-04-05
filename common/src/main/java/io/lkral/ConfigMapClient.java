package io.lkral;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

public class ConfigMapClient implements ResourceClient<ConfigMap, ConfigMapList> {
    private final MixedOperation<ConfigMap, ConfigMapList, Resource<ConfigMap>> configMapClient;
    private static ConfigMapClient client;

    private ConfigMapClient() {
        this.configMapClient = KubeClient.getInstance().getClient().configMaps();
    }

    public static ConfigMapClient client() {
        if (client == null) {
            client = new ConfigMapClient();
        }

        return client;
    }

    /**
     * Returns client for resource {@code ConfigMap}
     *
     * @return client of a MixedOperation<{@code ConfigMap}, {@code ConfigMapList}, Resource<{@code ConfigMap}>> resource
     */
    @Override
    public MixedOperation<ConfigMap, ConfigMapList, Resource<ConfigMap>> getClient() {
        return configMapClient;
    }

    /**
     * Creates specific {@code ConfigMap} resource
     *
     * @param resource {@code ConfigMap} resource
     */
    @Override
    public void create(ConfigMap resource) {
        configMapClient.resource(resource).create();
    }

    /**
     * Creates specific {@code ConfigMap} resource in specified namespace
     *
     * @param namespaceName name of Namespace, where the {@code ConfigMap} should be created
     * @param resource      {@code ConfigMap} resource
     */
    @Override
    public void createInNamespace(String namespaceName, ConfigMap resource) {
        configMapClient.inNamespace(namespaceName).resource(resource).create();
    }

    /**
     * Gets {@code ConfigMap} resource from Namespace in current context
     *
     * @param resourceName name of the {@code ConfigMap} resource
     * @return resource {@code ConfigMap}
     */
    @Override
    public ConfigMap get(String resourceName) {
        return configMapClient.withName(resourceName).get();
    }

    /**
     * Gets {@code ConfigMap} resource from specified Namespace
     *
     * @param namespaceName name of Namespace, from where the {@code ConfigMap} should be obtained
     * @param resourceName  name of the {@code ConfigMap} resource
     * @return desired {@code ConfigMap} resource
     */
    @Override
    public ConfigMap getFromNamespace(String namespaceName, String resourceName) {
        return configMapClient.inNamespace(namespaceName).withName(resourceName).get();
    }

    /**
     * Updates specific {@code ConfigMap} resource
     *
     * @param resource {@code ConfigMap} resource that will be updated
     */
    @Override
    public void update(ConfigMap resource) {
        configMapClient.resource(resource).update();
    }

    /**
     * Updates specific {@code ConfigMap} resource in specified Namespace
     *
     * @param namespaceName name of Namespace, where the {@code ConfigMap} should be updated
     * @param resource      {@code ConfigMap} resource that will be updated
     */
    @Override
    public void updateInNamespace(String namespaceName, ConfigMap resource) {
        configMapClient.inNamespace(namespaceName).resource(resource).update();
    }

    /**
     * Deletes {@code ConfigMap} resource from Namespace in current context
     *
     * @param resourceName name of the {@code ConfigMap} that will be deleted
     */
    @Override
    public void delete(String resourceName) {
        configMapClient.withName(resourceName).delete();
    }

    /**
     * Deletes {@code ConfigMap} resource from specified Namespace
     *
     * @param namespaceName name of Namespace, from where the {@code ConfigMap} will be deleted
     * @param resourceName  name of the {@code ConfigMap} resource
     */
    @Override
    public void deleteFromNamespace(String namespaceName, String resourceName) {
        configMapClient.inNamespace(namespaceName).withName(resourceName).delete();
    }
}
