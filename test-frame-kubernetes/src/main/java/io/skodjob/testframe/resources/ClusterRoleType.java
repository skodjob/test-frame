/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.resources;

import java.util.function.Consumer;

import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleList;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.interfaces.ResourceType;

/**
 * Implementation of ResourceType for specific kubernetes resource
 */
public class ClusterRoleType implements ResourceType<ClusterRole> {

    private final NonNamespaceOperation<ClusterRole, ClusterRoleList, Resource<ClusterRole>> client;

    /**
     * Constructor
     */
    public ClusterRoleType() {
        this.client = KubeResourceManager.getKubeClient().getClient().rbac().clusterRoles();
    }

    /**
     * Kind of api resource
     *
     * @return kind name
     */
    @Override
    public String getKind() {
        return "ClusterRole";
    }

    /**
     * Get specific client for resoruce
     *
     * @return specific client
     */
    @Override
    public NonNamespaceOperation<?, ?, ?> getClient() {
        return client;
    }

    /**
     * Creates specific {@link ClusterRole} resource
     *
     * @param resource {@link ClusterRole} resource
     */
    @Override
    public void create(ClusterRole resource) {
        client.resource(resource).create();
    }

    /**
     * Updates specific {@link ClusterRole} resource
     *
     * @param resource {@link ClusterRole} resource that will be updated
     */
    @Override
    public void update(ClusterRole resource) {
        client.resource(resource).update();
    }

    /**
     * Deletes {@link ClusterRole} resource from Namespace in current context
     *
     * @param resourceName name of the {@link ClusterRole} that will be deleted
     */
    @Override
    public void delete(String resourceName) {
        client.withName(resourceName).delete();
    }

    /**
     * Replaces {@link ClusterRole} resource using {@link Consumer}
     * from which is the current {@link ClusterRole} resource updated
     *
     * @param resourceName name of the {@link ClusterRole} that will be replaced
     * @param editor       {@link Consumer} containing updates to the resource
     */
    @Override
    public void replace(String resourceName, Consumer<ClusterRole> editor) {
        ClusterRole toBeUpdated = client.withName(resourceName).get();
        editor.accept(toBeUpdated);
        update(toBeUpdated);
    }

    /**
     * Waits for {@link ClusterRole} to be ready (created/running)
     *
     * @param resource resource
     * @return result of the readiness check
     */
    @Override
    public boolean waitForReadiness(ClusterRole resource) {
        return resource != null;
    }

    /**
     * Waits for {@link ClusterRole} to be deleted
     *
     * @param resource resource
     * @return result of the deletion
     */
    @Override
    public boolean waitForDeletion(ClusterRole resource) {
        return resource == null;
    }
}
