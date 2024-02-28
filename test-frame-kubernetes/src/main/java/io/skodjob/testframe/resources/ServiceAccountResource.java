/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.resources;

import java.util.function.Consumer;

import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.interfaces.NamespacedResourceType;

public class ServiceAccountResource implements NamespacedResourceType<ServiceAccount> {

    private final MixedOperation<ServiceAccount, ServiceAccountList, Resource<ServiceAccount>> client;

    public ServiceAccountResource() {
        this.client = ResourceManager.getKubeClient().getClient().serviceAccounts();
    }

    /**
     * Kind of api resource
     * @return kind name
     */
    @Override
    public String getKind() {
        return "ServiceAccount";
    }

    /**
     * Creates specific {@link ServiceAccount} resource
     *
     * @param resource {@link ServiceAccount} resource
     */
    @Override
    public void create(ServiceAccount resource) {
        client.resource(resource).create();
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
     * Updates specific {@link ServiceAccount} resource
     *
     * @param resource {@link ServiceAccount} resource that will be updated
     */
    @Override
    public void update(ServiceAccount resource) {
        client.resource(resource).update();
    }

    /**
     * Deletes {@link ServiceAccount} resource from Namespace in current context
     *
     * @param resourceName name of the {@link ServiceAccount} that will be deleted
     */
    @Override
    public void delete(String resourceName) {
        client.withName(resourceName).delete();
    }

    /**
     * Replaces {@link ServiceAccount} resource using {@link Consumer}
     * from which is the current {@link ServiceAccount} resource updated
     *
     * @param resourceName name of the {@link ServiceAccount} that will be replaced
     * @param editor       {@link Consumer} containing updates to the resource
     */
    @Override
    public void replace(String resourceName, Consumer<ServiceAccount> editor) {
        ServiceAccount toBeReplaced = client.withName(resourceName).get();
        editor.accept(toBeReplaced);
        update(toBeReplaced);
    }

    /**
     * Waits for {@link ServiceAccount} to be ready (created/running)
     *
     * @param resource
     * @return result of the readiness check
     */
    @Override
    public boolean waitForReadiness(ServiceAccount resource) {
        return resource != null;
    }

    /**
     * Waits for {@link ServiceAccount} to be deleted
     *
     * @param resource
     * @return result of the deletion
     */
    @Override
    public boolean waitForDeletion(ServiceAccount resource) {
        return resource == null;
    }

    /**
     * Creates specific {@link ServiceAccount} resource in Namespace specified by user
     *
     * @param namespaceName Namespace, where the resource should be created
     * @param resource      {@link ServiceAccount} resource
     */
    @Override
    public void createInNamespace(String namespaceName, ServiceAccount resource) {
        client.inNamespace(namespaceName).resource(resource).create();
    }

    /**
     * Updates specific {@link ServiceAccount} resource in Namespace specified by user
     *
     * @param namespaceName Namespace, where the resource should be updated
     * @param resource      {@link ServiceAccount} updated resource
     */
    @Override
    public void updateInNamespace(String namespaceName, ServiceAccount resource) {
        client.inNamespace(namespaceName).resource(resource).update();
    }

    /**
     * Deletes {@link ServiceAccount} resource from Namespace specified by user
     *
     * @param namespaceName Namespace, where the resource should be deleted
     * @param resourceName  name of the {@link ServiceAccount} that will be deleted
     */
    @Override
    public void deleteFromNamespace(String namespaceName, String resourceName) {
        client.inNamespace(namespaceName).withName(resourceName).delete();
    }

    /**
     * Replaces {@link ServiceAccount} resource in Namespace specified by user, using {@link Consumer}
     * from which is the current {@link ServiceAccount} resource updated
     *
     * @param namespaceName Namespace, where the resource should be replaced
     * @param resourceName  name of the {@link ServiceAccount} that will be replaced
     * @param editor        {@link Consumer} containing updates to the resource
     */
    @Override
    public void replaceInNamespace(String namespaceName, String resourceName, Consumer<ServiceAccount> editor) {
        ServiceAccount toBeReplaced = client.inNamespace(namespaceName).withName(resourceName).get();
        editor.accept(toBeReplaced);
        updateInNamespace(namespaceName, toBeReplaced);
    }
}
