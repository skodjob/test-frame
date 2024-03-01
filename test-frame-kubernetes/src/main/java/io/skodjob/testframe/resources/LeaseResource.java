/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.resources;

import java.util.function.Consumer;

import io.fabric8.kubernetes.api.model.coordination.v1.Lease;
import io.fabric8.kubernetes.api.model.coordination.v1.LeaseList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.interfaces.NamespacedResourceType;

/**
 * Implementation of ResourceType for specific kubernetes resource
 */
public class LeaseResource implements NamespacedResourceType<Lease> {

    private MixedOperation<Lease, LeaseList, Resource<Lease>> client;

    /**
     * Constructor
     */
    public LeaseResource() {
        this.client = KubeResourceManager.getKubeClient().getClient().leases();
    }

    /**
     * Kind of api resource
     * @return kind name
     */
    @Override
    public String getKind() {
        return "Lease";
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
     * Creates specific {@link Lease} resource
     *
     * @param resource {@link Lease} resource
     */
    @Override
    public void create(Lease resource) {
        client.resource(resource).create();
    }

    /**
     * Updates specific {@link Lease} resource
     *
     * @param resource {@link Lease} resource that will be updated
     */
    @Override
    public void update(Lease resource) {
        client.resource(resource).update();
    }

    /**
     * Deletes {@link Lease} resource from Namespace in current context
     *
     * @param resourceName name of the {@link Lease} that will be deleted
     */
    @Override
    public void delete(String resourceName) {
        client.withName(resourceName).delete();
    }

    /**
     * Replaces {@link Lease} resource using {@link Consumer}
     * from which is the current {@link Lease} resource updated
     *
     * @param resourceName name of the {@link Lease} that will be replaced
     * @param editor       {@link Consumer} containing updates to the resource
     */
    @Override
    public void replace(String resourceName, Consumer<Lease> editor) {
        Lease toBeUpdated = client.withName(resourceName).get();
        editor.accept(toBeUpdated);
        update(toBeUpdated);
    }

    /**
     * Waits for {@link Lease} to be ready (created/running)
     *
     * @param resource resource
     * @return result of the readiness check
     */
    @Override
    public boolean waitForReadiness(Lease resource) {
        return client.resource(resource).isReady();
    }

    /**
     * Waits for {@link Lease} to be deleted
     *
     * @param resource resource
     * @return result of the deletion
     */
    @Override
    public boolean waitForDeletion(Lease resource) {
        return resource == null;
    }

    /**
     * Creates specific {@link Lease} resource in Namespace specified by user
     *
     * @param namespaceName Namespace, where the resource should be created
     * @param resource      {@link Lease} resource
     */
    @Override
    public void createInNamespace(String namespaceName, Lease resource) {
        client.inNamespace(namespaceName).resource(resource).create();
    }

    /**
     * Updates specific {@link Lease} resource in Namespace specified by user
     *
     * @param namespaceName Namespace, where the resource should be updated
     * @param resource      {@link Lease} updated resource
     */
    @Override
    public void updateInNamespace(String namespaceName, Lease resource) {
        client.inNamespace(namespaceName).resource(resource).update();
    }

    /**
     * Deletes {@link Lease} resource from Namespace specified by user
     *
     * @param namespaceName Namespace, where the resource should be deleted
     * @param resourceName  name of the {@link Lease} that will be deleted
     */
    @Override
    public void deleteFromNamespace(String namespaceName, String resourceName) {
        client.inNamespace(namespaceName).withName(resourceName).delete();
    }

    /**
     * Replaces {@link Lease} resource in Namespace specified by user, using {@link Consumer}
     * from which is the current {@link Lease} resource updated
     *
     * @param namespaceName Namespace, where the resource should be replaced
     * @param resourceName  name of the {@link Lease} that will be replaced
     * @param editor        {@link Consumer} containing updates to the resource
     */
    @Override
    public void replaceInNamespace(String namespaceName, String resourceName, Consumer<Lease> editor) {
        Lease toBeReplaced = client.inNamespace(namespaceName).withName(resourceName).get();
        editor.accept(toBeReplaced);
        updateInNamespace(namespaceName, toBeReplaced);
    }
}
