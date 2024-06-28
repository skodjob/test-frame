/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.resources;

import java.util.function.Consumer;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import io.skodjob.testframe.interfaces.NamespacedResourceType;

/**
 * Implementation of ResourceType for specific kubernetes resource
 */
public class DeploymentType implements NamespacedResourceType<Deployment> {

    private final MixedOperation<Deployment, DeploymentList, RollableScalableResource<Deployment>> client;

    /**
     * Constructor
     */
    public DeploymentType() {
        this.client = KubeResourceManager.getKubeClient().getClient().apps().deployments();
    }

    /**
     * Kind of api resource
     *
     * @return kind name
     */
    @Override
    public String getKind() {
        return "Deployment";
    }

    /**
     * Get specific client for resoruce
     *
     * @return specific client
     */
    @Override
    public MixedOperation<?, ?, ?> getClient() {
        return client;
    }

    /**
     * Creates specific {@link Deployment} resource
     *
     * @param resource {@link Deployment} resource
     */
    @Override
    public void create(Deployment resource) {
        client.resource(resource).create();
    }

    /**
     * Updates specific {@link Deployment} resource
     *
     * @param resource {@link Deployment} resource that will be updated
     */
    @Override
    public void update(Deployment resource) {
        client.resource(resource).update();
    }

    /**
     * Deletes {@link Deployment} resource from Namespace in current context
     *
     * @param resourceName name of the {@link Deployment} that will be deleted
     */
    @Override
    public void delete(String resourceName) {
        client.withName(resourceName).delete();
    }

    /**
     * Replaces {@link Deployment} resource using {@link Consumer}
     * from which is the current {@link Deployment} resource updated
     *
     * @param resourceName name of the {@link Deployment} that will be replaced
     * @param editor       {@link Consumer} containing updates to the resource
     */
    @Override
    public void replace(String resourceName, Consumer<Deployment> editor) {
        Deployment toBeUpdated = client.withName(resourceName).get();
        editor.accept(toBeUpdated);
        update(toBeUpdated);
    }

    /**
     * Waits for {@link Deployment} to be ready (created/running)
     *
     * @param resource resource
     * @return result of the readiness check
     */
    @Override
    public boolean waitForReadiness(Deployment resource) {
        return client.resource(resource).isReady();
    }

    /**
     * Waits for {@link Deployment} to be deleted
     *
     * @param resource resource
     * @return result of the deletion
     */
    @Override
    public boolean waitForDeletion(Deployment resource) {
        return resource == null;
    }

    /**
     * Creates specific {@link Deployment} resource in Namespace specified by user
     *
     * @param namespaceName Namespace, where the resource should be created
     * @param resource      {@link Deployment} resource
     */
    @Override
    public void createInNamespace(String namespaceName, Deployment resource) {
        client.inNamespace(namespaceName).resource(resource).create();
    }

    /**
     * Updates specific {@link Deployment} resource in Namespace specified by user
     *
     * @param namespaceName Namespace, where the resource should be updated
     * @param resource      {@link Deployment} updated resource
     */
    @Override
    public void updateInNamespace(String namespaceName, Deployment resource) {
        client.inNamespace(namespaceName).resource(resource).update();
    }

    /**
     * Deletes {@link Deployment} resource from Namespace specified by user
     *
     * @param namespaceName Namespace, where the resource should be deleted
     * @param resourceName  name of the {@link Deployment} that will be deleted
     */
    @Override
    public void deleteFromNamespace(String namespaceName, String resourceName) {
        client.inNamespace(namespaceName).withName(resourceName).delete();
    }

    /**
     * Replaces {@link Deployment} resource in Namespace specified by user, using {@link Consumer}
     * from which is the current {@link Deployment} resource updated
     *
     * @param namespaceName Namespace, where the resource should be replaced
     * @param resourceName  name of the {@link Deployment} that will be replaced
     * @param editor        {@link Consumer} containing updates to the resource
     */
    @Override
    public void replaceInNamespace(String namespaceName, String resourceName, Consumer<Deployment> editor) {
        Deployment toBeUpdated = client.inNamespace(namespaceName).withName(resourceName).get();
        editor.accept(toBeUpdated);
        updateInNamespace(namespaceName, toBeUpdated);
    }
}
