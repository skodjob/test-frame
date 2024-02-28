/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.resources;

import java.util.function.Consumer;

import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.interfaces.NamespacedResourceType;

public class NetworkPolicyResource implements NamespacedResourceType<NetworkPolicy> {

    private final MixedOperation<NetworkPolicy, NetworkPolicyList, Resource<NetworkPolicy>> client;

    public NetworkPolicyResource() {
        this.client = ResourceManager.getKubeClient().getClient().network().networkPolicies();
    }

    /**
     * Kind of api resource
     * @return kind name
     */
    @Override
    public String getKind() {
        return "NetworkPolicy";
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
     * Creates specific {@link NetworkPolicy} resource
     *
     * @param resource {@link NetworkPolicy} resource
     */
    @Override
    public void create(NetworkPolicy resource) {
        client.resource(resource).create();
    }

    /**
     * Updates specific {@link NetworkPolicy} resource
     *
     * @param resource {@link NetworkPolicy} resource that will be updated
     */
    @Override
    public void update(NetworkPolicy resource) {
        client.resource(resource).update();
    }

    /**
     * Deletes {@link NetworkPolicy} resource from Namespace in current context
     *
     * @param resourceName name of the {@link NetworkPolicy} that will be deleted
     */
    @Override
    public void delete(String resourceName) {
        client.withName(resourceName).delete();
    }

    /**
     * Replaces {@link NetworkPolicy} resource using {@link Consumer}
     * from which is the current {@link NetworkPolicy} resource updated
     *
     * @param resourceName name of the {@link NetworkPolicy} that will be replaced
     * @param editor       {@link Consumer} containing updates to the resource
     */
    @Override
    public void replace(String resourceName, Consumer<NetworkPolicy> editor) {
        NetworkPolicy toBeUpdated = client.withName(resourceName).get();
        editor.accept(toBeUpdated);
        update(toBeUpdated);
    }

    /**
     * Waits for {@link NetworkPolicy} to be ready (created/running)
     *
     * @param resource
     * @return result of the readiness check
     */
    @Override
    public boolean waitForReadiness(NetworkPolicy resource) {
        return resource != null;
    }

    /**
     * Waits for {@link NetworkPolicy} to be deleted
     *
     * @param resource
     * @return result of the deletion
     */
    @Override
    public boolean waitForDeletion(NetworkPolicy resource) {
        return resource == null;
    }

    /**
     * Creates specific {@link NetworkPolicy} resource in Namespace specified by user
     *
     * @param namespaceName Namespace, where the resource should be created
     * @param resource      {@link NetworkPolicy} resource
     */
    @Override
    public void createInNamespace(String namespaceName, NetworkPolicy resource) {
        client.inNamespace(namespaceName).resource(resource).create();
    }

    /**
     * Updates specific {@link NetworkPolicy} resource in Namespace specified by user
     *
     * @param namespaceName Namespace, where the resource should be updated
     * @param resource      {@link NetworkPolicy} updated resource
     */
    @Override
    public void updateInNamespace(String namespaceName, NetworkPolicy resource) {
        client.inNamespace(namespaceName).resource(resource).update();
    }

    /**
     * Deletes {@link NetworkPolicy} resource from Namespace specified by user
     *
     * @param namespaceName Namespace, where the resource should be deleted
     * @param resourceName  name of the {@link NetworkPolicy} that will be deleted
     */
    @Override
    public void deleteFromNamespace(String namespaceName, String resourceName) {
        client.inNamespace(namespaceName).withName(resourceName).delete();
    }

    /**
     * Replaces {@link NetworkPolicy} resource in Namespace specified by user, using {@link Consumer}
     * from which is the current {@link NetworkPolicy} resource updated
     *
     * @param namespaceName Namespace, where the resource should be replaced
     * @param resourceName  name of the {@link NetworkPolicy} that will be replaced
     * @param editor        {@link Consumer} containing updates to the resource
     */
    @Override
    public void replaceInNamespace(String namespaceName, String resourceName, Consumer<NetworkPolicy> editor) {
        NetworkPolicy toBeReplaced = client.inNamespace(namespaceName).withName(resourceName).get();
        editor.accept(toBeReplaced);
        updateInNamespace(namespaceName, toBeReplaced);
    }
}
