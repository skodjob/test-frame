package io.skodjob.resources;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.clients.KubeClient;
import io.skodjob.interfaces.ResourceType;

import java.util.function.Consumer;

public class NamespaceResource implements ResourceType<Namespace, NamespaceList, Resource<Namespace>> {

    private final NonNamespaceOperation<Namespace, NamespaceList, Resource<Namespace>> client;

    public NamespaceResource() {
        this.client = KubeClient.getInstance().getClient().namespaces();
    }

    /**
     * Returns client for resource {@link Namespace}
     *
     * @return client of a MixedOperation<{@link Namespace}, {@link NamespaceList}, Resource<{@link Namespace}>> resource
     */
    @Override
    public NonNamespaceOperation<Namespace, NamespaceList, Resource<Namespace>> getClient() {
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
