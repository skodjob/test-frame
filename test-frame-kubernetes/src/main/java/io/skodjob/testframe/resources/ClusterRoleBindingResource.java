/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.resources;

import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBindingList;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.interfaces.ResourceType;

import java.util.function.Consumer;

public class ClusterRoleBindingResource implements ResourceType<ClusterRoleBinding> {

    private final NonNamespaceOperation<ClusterRoleBinding, ClusterRoleBindingList, Resource<ClusterRoleBinding>> client;

    public ClusterRoleBindingResource() {
        this.client = ResourceManager.getKubeClient().getClient().rbac().clusterRoleBindings();
    }

    /**
     * Get specific client for resoruce
     * @return specific client
     */
    @Override
    public NonNamespaceOperation<?, ?, ?> getClient() {
        return client;
    }

    /**
     * Kind of api resource
     * @return kind name
     */
    @Override
    public String getKind() {
        return "ClusterRoleBinding";
    }

    /**
     * Creates specific {@link ClusterRoleBinding} resource
     *
     * @param resource {@link ClusterRoleBinding} resource
     */
    @Override
    public void create(ClusterRoleBinding resource) {
        client.resource(resource).create();
    }

    /**
     * Updates specific {@link ClusterRoleBinding} resource
     *
     * @param resource {@link ClusterRoleBinding} resource that will be updated
     */
    @Override
    public void update(ClusterRoleBinding resource) {
        client.resource(resource).update();
    }

    /**
     * Deletes {@link ClusterRoleBinding} resource from Namespace in current context
     *
     * @param resourceName name of the {@link ClusterRoleBinding} that will be deleted
     */
    @Override
    public void delete(String resourceName) {
        client.withName(resourceName).delete();
    }

    /**
     * Replaces {@link ClusterRoleBinding} resource using {@link Consumer}
     * from which is the current {@link ClusterRoleBinding} resource updated
     *
     * @param resourceName name of the {@link ClusterRoleBinding} that will be replaced
     * @param editor       {@link Consumer} containing updates to the resource
     */
    @Override
    public void replace(String resourceName, Consumer<ClusterRoleBinding> editor) {
        ClusterRoleBinding toBeUpdated = client.withName(resourceName).get();
        editor.accept(toBeUpdated);
        update(toBeUpdated);
    }

    /**
     * Waits for {@link ClusterRoleBinding} to be ready (created/running)
     *
     * @param resource
     * @return result of the readiness check
     */
    @Override
    public boolean waitForReadiness(ClusterRoleBinding resource) {
        return resource != null;
    }

    /**
     * Waits for {@link ClusterRoleBinding} to be deleted
     *
     * @param resource
     * @return result of the deletion
     */
    @Override
    public boolean waitForDeletion(ClusterRoleBinding resource) {
        return resource == null;
    }
}
