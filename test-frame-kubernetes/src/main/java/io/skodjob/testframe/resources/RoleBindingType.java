/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.resources;

import java.util.function.Consumer;

import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.interfaces.NamespacedResourceType;

/**
 * Implementation of ResourceType for specific kubernetes resource
 */
public class RoleBindingType implements NamespacedResourceType<RoleBinding> {

    private final MixedOperation<RoleBinding, RoleBindingList, Resource<RoleBinding>> client;

    /**
     * Constructor
     */
    public RoleBindingType() {
        this.client = KubeResourceManager.getKubeClient().getClient().rbac().roleBindings();
    }

    /**
     * Kind of api resource
     * @return kind name
     */
    @Override
    public String getKind() {
        return "RoleBinding";
    }

    /**
     * Get specific client for resoruce
     * @return specific client
     */
    @Override
    public MixedOperation<?, ?, ?> getClient() {
        return client;
    }

    /**
     * Creates specific {@link RoleBinding} resource
     *
     * @param resource {@link RoleBinding} resource
     */
    @Override
    public void create(RoleBinding resource) {
        client.resource(resource).create();
    }

    /**
     * Updates specific {@link RoleBinding} resource
     *
     * @param resource {@link RoleBinding} resource that will be updated
     */
    @Override
    public void update(RoleBinding resource) {
        client.resource(resource).update();
    }

    /**
     * Deletes {@link RoleBinding} resource from Namespace in current context
     *
     * @param resourceName name of the {@link RoleBinding} that will be deleted
     */
    @Override
    public void delete(String resourceName) {
        client.withName(resourceName).delete();
    }

    /**
     * Replaces {@link RoleBinding} resource using {@link Consumer}
     * from which is the current {@link RoleBinding} resource updated
     *
     * @param resourceName name of the {@link RoleBinding} that will be replaced
     * @param editor       {@link Consumer} containing updates to the resource
     */
    @Override
    public void replace(String resourceName, Consumer<RoleBinding> editor) {
        RoleBinding toBeUpdated = client.withName(resourceName).get();
        editor.accept(toBeUpdated);
        update(toBeUpdated);
    }

    /**
     * Waits for {@link RoleBinding} to be ready (created/running)
     *
     * @param resource resource
     * @return result of the readiness check
     */
    @Override
    public boolean waitForReadiness(RoleBinding resource) {
        return resource != null;
    }

    /**
     * Waits for {@link RoleBinding} to be deleted
     *
     * @param resource resource
     * @return result of the deletion
     */
    @Override
    public boolean waitForDeletion(RoleBinding resource) {
        return resource == null;
    }

    /**
     * Creates specific {@link RoleBinding} resource in Namespace specified by user
     *
     * @param namespaceName Namespace, where the resource should be created
     * @param resource      {@link RoleBinding} resource
     */
    @Override
    public void createInNamespace(String namespaceName, RoleBinding resource) {
        client.inNamespace(namespaceName).resource(resource).create();
    }

    /**
     * Updates specific {@link RoleBinding} resource in Namespace specified by user
     *
     * @param namespaceName Namespace, where the resource should be updated
     * @param resource      {@link RoleBinding} updated resource
     */
    @Override
    public void updateInNamespace(String namespaceName, RoleBinding resource) {
        client.inNamespace(namespaceName).resource(resource).update();
    }

    /**
     * Deletes {@link RoleBinding} resource from Namespace specified by user
     *
     * @param namespaceName Namespace, where the resource should be deleted
     * @param resourceName  name of the {@link RoleBinding} that will be deleted
     */
    @Override
    public void deleteFromNamespace(String namespaceName, String resourceName) {
        client.inNamespace(namespaceName).withName(resourceName).delete();
    }

    /**
     * Replaces {@link RoleBinding} resource in Namespace specified by user, using {@link Consumer}
     * from which is the current {@link RoleBinding} resource updated
     *
     * @param namespaceName Namespace, where the resource should be replaced
     * @param resourceName  name of the {@link RoleBinding} that will be replaced
     * @param editor        {@link Consumer} containing updates to the resource
     */
    @Override
    public void replaceInNamespace(String namespaceName, String resourceName, Consumer<RoleBinding> editor) {
        RoleBinding toBeReplaced = client.inNamespace(namespaceName).withName(resourceName).get();
        editor.accept(toBeReplaced);
        updateInNamespace(namespaceName, toBeReplaced);
    }
}
