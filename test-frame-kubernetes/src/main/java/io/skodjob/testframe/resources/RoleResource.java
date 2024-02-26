/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.resources;

import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.interfaces.NamespacedResourceType;

import java.util.function.Consumer;

public class RoleResource implements NamespacedResourceType<Role> {

    private final MixedOperation<Role, RoleList, Resource<Role>> client;

    public RoleResource() {
        this.client = ResourceManager.getKubeClient().getClient().rbac().roles();
    }

    /**
     * Kind of api resource
     * @return kind name
     */
    @Override
    public String getKind() {
        return "Role";
    }

    /**
     * Creates specific {@link Role} resource
     *
     * @param resource {@link Role} resource
     */
    @Override
    public void create(Role resource) {
        client.resource(resource).create();
    }

    /**
     * Updates specific {@link Role} resource
     *
     * @param resource {@link Role} resource that will be updated
     */
    @Override
    public void update(Role resource) {
        client.resource(resource).update();
    }

    /**
     * Deletes {@link Role} resource from Namespace in current context
     *
     * @param resourceName name of the {@link Role} that will be deleted
     */
    @Override
    public void delete(String resourceName) {
        client.withName(resourceName).delete();
    }

    /**
     * Replaces {@link Role} resource using {@link Consumer}
     * from which is the current {@link Role} resource updated
     *
     * @param resourceName name of the {@link Role} that will be replaced
     * @param editor       {@link Consumer} containing updates to the resource
     */
    @Override
    public void replace(String resourceName, Consumer<Role> editor) {
        Role toBeUpdated = client.withName(resourceName).get();
        editor.accept(toBeUpdated);
        update(toBeUpdated);
    }

    /**
     * Waits for {@link Role} to be ready (created/running)
     *
     * @param resource
     * @return result of the readiness check
     */
    @Override
    public boolean waitForReadiness(Role resource) {
        return resource != null;
    }

    /**
     * Waits for {@link Role} to be deleted
     *
     * @param resource
     * @return result of the deletion
     */
    @Override
    public boolean waitForDeletion(Role resource) {
        return resource == null;
    }

    /**
     * Creates specific {@link Role} resource in Namespace specified by user
     *
     * @param namespaceName Namespace, where the resource should be created
     * @param resource      {@link Role} resource
     */
    @Override
    public void createInNamespace(String namespaceName, Role resource) {
        client.inNamespace(namespaceName).resource(resource).create();
    }

    /**
     * Updates specific {@link Role} resource in Namespace specified by user
     *
     * @param namespaceName Namespace, where the resource should be updated
     * @param resource      {@link Role} updated resource
     */
    @Override
    public void updateInNamespace(String namespaceName, Role resource) {
        client.inNamespace(namespaceName).resource(resource).update();
    }

    /**
     * Deletes {@link Role} resource from Namespace specified by user
     *
     * @param namespaceName Namespace, where the resource should be deleted
     * @param resourceName  name of the {@link Role} that will be deleted
     */
    @Override
    public void deleteFromNamespace(String namespaceName, String resourceName) {
        client.inNamespace(namespaceName).withName(resourceName).delete();
    }

    /**
     * Replaces {@link Role} resource in Namespace specified by user, using {@link Consumer}
     * from which is the current {@link Role} resource updated
     *
     * @param namespaceName Namespace, where the resource should be replaced
     * @param resourceName  name of the {@link Role} that will be replaced
     * @param editor        {@link Consumer} containing updates to the resource
     */
    @Override
    public void replaceInNamespace(String namespaceName, String resourceName, Consumer<Role> editor) {
        Role toBeReplaced = client.inNamespace(namespaceName).withName(resourceName).get();
        editor.accept(toBeReplaced);
        updateInNamespace(namespaceName, toBeReplaced);
    }
}
