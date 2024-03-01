/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.resources;

import java.util.function.Consumer;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.interfaces.NamespacedResourceType;

/**
 * Implementation of ResourceType for specific kubernetes resource
 */
public class SecretResource implements NamespacedResourceType<Secret> {

    private final MixedOperation<Secret, SecretList, Resource<Secret>> client;

    /**
     * Constructor
     */
    public SecretResource() {
        this.client = KubeResourceManager.getKubeClient().getClient().secrets();
    }

    /**
     * Kind of api resource
     * @return kind name
     */
    @Override
    public String getKind() {
        return "Secret";
    }

    /**
     * Creates specific {@link Secret} resource
     *
     * @param resource {@link Secret} resource
     */
    @Override
    public void create(Secret resource) {
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
     * Creates specific {@link Secret} resource in specified namespace
     *
     * @param namespaceName name of Namespace, where the {@link Secret} should be created
     * @param resource      {@link Secret} resource
     */
    @Override
    public void createInNamespace(String namespaceName, Secret resource) {
        client.inNamespace(namespaceName).resource(resource).create();
    }

    /**
     * Updates specific {@link Secret} resource
     *
     * @param resource {@link Secret} resource that will be updated
     */
    @Override
    public void update(Secret resource) {
        client.resource(resource).update();
    }

    /**
     * Updates specific {@link Secret} resource in specified Namespace
     *
     * @param namespaceName name of Namespace, where the {@link Secret} should be updated
     * @param resource      {@link Secret} resource that will be updated
     */
    @Override
    public void updateInNamespace(String namespaceName, Secret resource) {
        client.inNamespace(namespaceName).resource(resource).update();
    }

    /**
     * Deletes {@link Secret} resource from Namespace in current context
     *
     * @param resourceName name of the {@link Secret} that will be deleted
     */
    @Override
    public void delete(String resourceName) {
        client.withName(resourceName).delete();
    }

    /**
     * @param resourceName name
     * @param editor modifier
     */
    @Override
    public void replace(String resourceName, Consumer<Secret> editor) {
        Secret toBeReplaced = client.withName(resourceName).get();
        editor.accept(toBeReplaced);
        update(toBeReplaced);
    }

    /**
     * Waits for resource to be ready (created/running)
     *
     * @param resource resource
     * @return result of the readiness check
     */
    @Override
    public boolean waitForReadiness(Secret resource) {
        return false;
    }

    /**
     * Waits for resource to be deleted
     *
     * @param resource resource
     * @return result of the deletion
     */
    @Override
    public boolean waitForDeletion(Secret resource) {
        return false;
    }

    /**
     * Deletes {@link Secret} resource from specified Namespace
     *
     * @param namespaceName name of Namespace, from where the {@link Secret} will be deleted
     * @param resourceName  name of the {@link Secret} resource
     */
    @Override
    public void deleteFromNamespace(String namespaceName, String resourceName) {
        client.inNamespace(namespaceName).withName(resourceName).delete();
    }

    /**
     * @param namespaceName namespace
     * @param resourceName resource
     * @param editor modifier
     */
    @Override
    public void replaceInNamespace(String namespaceName, String resourceName, Consumer<Secret> editor) {
        Secret toBeReplaced = client.inNamespace(namespaceName).withName(resourceName).get();
        editor.accept(toBeReplaced);
        updateInNamespace(namespaceName, toBeReplaced);
    }
}
