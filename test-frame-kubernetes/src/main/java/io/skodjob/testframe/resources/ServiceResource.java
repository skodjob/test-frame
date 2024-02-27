/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.resources;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.skodjob.testframe.interfaces.NamespacedResourceType;

import java.util.function.Consumer;

public class ServiceResource implements NamespacedResourceType<Service> {

    private final MixedOperation<Service, ServiceList, io.fabric8.kubernetes.client.dsl.ServiceResource<Service>> client;

    public ServiceResource() {
        this.client = ResourceManager.getKubeClient().getClient().services();
    }

    /**
     * Kind of api resource
     * @return kind name
     */
    @Override
    public String getKind() {
        return "Service";
    }

    /**
     * Creates specific {@link Service} resource
     *
     * @param resource {@link Service} resource
     */
    @Override
    public void create(Service resource) {
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
     * Updates specific {@link Service} resource
     *
     * @param resource {@link Service} resource that will be updated
     */
    @Override
    public void update(Service resource) {
        client.resource(resource).update();
    }

    /**
     * Deletes {@link Service} resource from Namespace in current context
     *
     * @param resourceName name of the {@link Service} that will be deleted
     */
    @Override
    public void delete(String resourceName) {
        client.withName(resourceName).delete();
    }

    /**
     * Replaces {@link Service} resource using {@link Consumer}
     * from which is the current {@link Service} resource updated
     *
     * @param resourceName name of the {@link Service} that will be replaced
     * @param editor       {@link Consumer} containing updates to the resource
     */
    @Override
    public void replace(String resourceName, Consumer<Service> editor) {
        Service toBeReplaced = client.withName(resourceName).get();
        editor.accept(toBeReplaced);
        update(toBeReplaced);
    }

    /**
     * Waits for {@link Service} to be ready (created/running)
     *
     * @param resource
     * @return result of the readiness check
     */
    @Override
    public boolean waitForReadiness(Service resource) {
        return resource != null;
    }

    /**
     * Waits for {@link Service} to be deleted
     *
     * @param resource
     * @return result of the deletion
     */
    @Override
    public boolean waitForDeletion(Service resource) {
        return resource == null;
    }

    /**
     * Creates specific {@link Service} resource in Namespace specified by user
     *
     * @param namespaceName Namespace, where the resource should be created
     * @param resource      {@link Service} resource
     */
    @Override
    public void createInNamespace(String namespaceName, Service resource) {
        client.inNamespace(namespaceName).resource(resource).create();
    }

    /**
     * Updates specific {@link Service} resource in Namespace specified by user
     *
     * @param namespaceName Namespace, where the resource should be updated
     * @param resource      {@link Service} updated resource
     */
    @Override
    public void updateInNamespace(String namespaceName, Service resource) {
        client.inNamespace(namespaceName).resource(resource).update();
    }

    /**
     * Deletes {@link Service} resource from Namespace specified by user
     *
     * @param namespaceName Namespace, where the resource should be deleted
     * @param resourceName  name of the {@link Service} that will be deleted
     */
    @Override
    public void deleteFromNamespace(String namespaceName, String resourceName) {
        client.inNamespace(namespaceName).withName(resourceName).delete();
    }

    /**
     * Replaces {@link Service} resource in Namespace specified by user, using {@link Consumer}
     * from which is the current {@link Service} resource updated
     *
     * @param namespaceName Namespace, where the resource should be replaced
     * @param resourceName  name of the {@link Service} that will be replaced
     * @param editor        {@link Consumer} containing updates to the resource
     */
    @Override
    public void replaceInNamespace(String namespaceName, String resourceName, Consumer<Service> editor) {
        Service toBeReplaced = client.inNamespace(namespaceName).withName(resourceName).get();
        editor.accept(toBeReplaced);
        updateInNamespace(namespaceName, toBeReplaced);
    }
}
