/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.resources;

import java.util.function.Consumer;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.interfaces.ResourceType;

public class NamespaceResource implements ResourceType<Namespace> {

    private final NonNamespaceOperation<Namespace, NamespaceList, Resource<Namespace>> client;

    public NamespaceResource() {
        this.client = ResourceManager.getKubeClient().getClient().namespaces();
    }

    /**
     * Kind of api resource
     * @return kind name
     */
    @Override
    public String getKind() {
        return "Namespace";
    }

    /**
     * Get specific client for resoruce
     * @return specific client
     */
    @Override
    public NonNamespaceOperation<?, ?, ?> getClient() {
        return client;
    }

    public void create(String namespaceName) {
        Namespace namespace = new NamespaceBuilder()
            .withNewMetadata()
                .withName(namespaceName)
            .endMetadata()
            .build();

        create(namespace);
    }

    /**
     * Creates specific {@link Namespace} resource
     *
     * @param resource {@link Namespace} resource
     */
    @Override
    public void create(Namespace resource) {
        client.resource(resource).create();
    }

    /**
     * Updates specific {@link Namespace} resource
     *
     * @param resource {@link Namespace} resource that will be updated
     */
    @Override
    public void update(Namespace resource) {
        client.resource(resource).update();
    }

    /**
     * Deletes {@link Namespace} resource from Namespace in current context
     *
     * @param resourceName name of the {@link Namespace} that will be deleted
     */
    @Override
    public void delete(String resourceName) {
        client.withName(resourceName).delete();
    }

    /**
     * Replaces {@link Namespace} resource using {@link Consumer}
     * from which is the current {@link Namespace} resource updated
     *
     * @param resourceName name of the {@link Namespace} that will be replaced
     * @param editor       {@link Consumer} containing updates to the resource
     */
    @Override
    public void replace(String resourceName, Consumer<Namespace> editor) {
        Namespace toBeUpdated = client.withName(resourceName).get();
        editor.accept(toBeUpdated);
        update(toBeUpdated);
    }

    /**
     * Waits for {@link Namespace} to be ready (created/running)
     *
     * @param resource
     * @return result of the readiness check
     */
    @Override
    public boolean waitForReadiness(Namespace resource) {
        return resource != null;
    }

    /**
     * Waits for {@link Namespace} to be deleted
     *
     * @param resource
     * @return result of the deletion
     */
    @Override
    public boolean waitForDeletion(Namespace resource) {
        return resource == null;
    }
}
