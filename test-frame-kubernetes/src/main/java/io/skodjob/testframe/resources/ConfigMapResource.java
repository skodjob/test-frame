/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.resources;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.interfaces.NamespacedResourceType;

import java.util.function.Consumer;

public class ConfigMapResource implements NamespacedResourceType<ConfigMap> {
    private final MixedOperation<ConfigMap, ConfigMapList, Resource<ConfigMap>> client;

    public ConfigMapResource() {
        this.client = ResourceManager.getKubeClient().getClient().configMaps();
    }

    /**
     * Kind of api resource
     * @return kind name
     */
    @Override
    public String getKind() {
        return "ConfigMap";
    }

    /**
     * Creates specific {@link ConfigMap} resource in Namespace specified by user
     *
     * @param namespaceName Namespace, where the resource should be created
     * @param resource      {@link ConfigMap} resource
     */
    @Override
    public void createInNamespace(String namespaceName, ConfigMap resource) {
        client.inNamespace(namespaceName).resource(resource).create();
    }

    /**
     * Updates specific {@link ConfigMap} resource in Namespace specified by user
     *
     * @param namespaceName Namespace, where the resource should be updated
     * @param resource      {@link ConfigMap} updated resource
     */
    @Override
    public void updateInNamespace(String namespaceName, ConfigMap resource) {
        client.inNamespace(namespaceName).resource(resource).update();
    }

    /**
     * Deletes {@link ConfigMap} resource from Namespace specified by user
     *
     * @param namespaceName Namespace, where the resource should be deleted
     * @param resourceName  name of the {@link ConfigMap} that will be deleted
     */
    @Override
    public void deleteFromNamespace(String namespaceName, String resourceName) {
        client.inNamespace(namespaceName).withName(resourceName).delete();
    }

    /**
     * Replaces {@link ConfigMap} resource in Namespace specified by user, using {@link Consumer}
     * from which is the current {@link ConfigMap} resource updated
     *
     * @param namespaceName Namespace, where the resource should be replaced
     * @param resourceName  name of the {@link ConfigMap} that will be replaced
     * @param editor        {@link Consumer} containing updates to the resource
     */
    @Override
    public void replaceInNamespace(String namespaceName, String resourceName, Consumer<ConfigMap> editor) {
        ConfigMap toBeUpdated = client.inNamespace(namespaceName).withName(resourceName).get();
        editor.accept(toBeUpdated);
        updateInNamespace(namespaceName, toBeUpdated);
    }

    /**
     * Creates specific {@link ConfigMap} resource
     *
     * @param resource {@link ConfigMap} resource
     */
    @Override
    public void create(ConfigMap resource) {
        client.resource(resource).create();
    }

    /**
     * Updates specific {@link ConfigMap} resource
     *
     * @param resource {@link ConfigMap} resource that will be updated
     */
    @Override
    public void update(ConfigMap resource) {
        client.resource(resource).update();
    }

    /**
     * Deletes {@link ConfigMap} resource from Namespace in current context
     *
     * @param resourceName name of the {@link ConfigMap} that will be deleted
     */
    @Override
    public void delete(String resourceName) {
        client.withName(resourceName).delete();
    }

    /**
     * Replaces {@link ConfigMap} resource using {@link Consumer}
     * from which is the current {@link ConfigMap} resource updated
     *
     * @param resourceName name of the {@link ConfigMap} that will be replaced
     * @param editor       {@link Consumer} containing updates to the resource
     */
    @Override
    public void replace(String resourceName, Consumer<ConfigMap> editor) {
        ConfigMap toBeUpdated = client.withName(resourceName).get();
        editor.accept(toBeUpdated);
        update(toBeUpdated);
    }

    /**
     * Waits for {@link ConfigMap} to be ready (created/running)
     *
     * @param resource
     * @return result of the readiness check
     */
    @Override
    public boolean waitForReadiness(ConfigMap resource) {
        return resource != null;
    }

    /**
     * Waits for {@link ConfigMap} to be deleted
     *
     * @param resource
     * @return result of the deletion
     */
    @Override
    public boolean waitForDeletion(ConfigMap resource) {
        return resource == null;
    }
}
