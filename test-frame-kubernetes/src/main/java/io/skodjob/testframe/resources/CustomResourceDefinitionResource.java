package io.skodjob.testframe.resources;

import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionList;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.clients.KubeClient;
import io.skodjob.testframe.interfaces.ResourceType;

import java.util.function.Consumer;

public class CustomResourceDefinitionResource implements ResourceType<CustomResourceDefinition, CustomResourceDefinitionList, Resource<CustomResourceDefinition>> {

    private final NonNamespaceOperation<CustomResourceDefinition, CustomResourceDefinitionList, Resource<CustomResourceDefinition>> client;

    public CustomResourceDefinitionResource() {
        this.client = KubeClient.getInstance().getClient().apiextensions().v1().customResourceDefinitions();
    }
    /**
     * Returns client for resource {@link CustomResourceDefinition}
     *
     * @return client of a MixedOperation<{@link CustomResourceDefinition}, {@link CustomResourceDefinitionList}, Resource<{@link CustomResourceDefinition}>> resource
     */
    @Override
    public NonNamespaceOperation<CustomResourceDefinition, CustomResourceDefinitionList, Resource<CustomResourceDefinition>> getClient() {
        return client;
    }

    /**
     * Creates specific {@link CustomResourceDefinition} resource
     *
     * @param resource {@link CustomResourceDefinition} resource
     */
    @Override
    public void create(CustomResourceDefinition resource) {
        client.resource(resource).create();
    }

    /**
     * Updates specific {@link CustomResourceDefinition} resource
     *
     * @param resource {@link CustomResourceDefinition} resource that will be updated
     */
    @Override
    public void update(CustomResourceDefinition resource) {
        client.resource(resource).update();
    }

    /**
     * Deletes {@link CustomResourceDefinition} resource from Namespace in current context
     *
     * @param resourceName name of the {@link CustomResourceDefinition} that will be deleted
     */
    @Override
    public void delete(String resourceName) {
        client.withName(resourceName).delete();
    }

    /**
     * Replaces {@link CustomResourceDefinition} resource using {@link Consumer}
     * from which is the current {@link CustomResourceDefinition} resource updated
     *
     * @param resourceName name of the {@link CustomResourceDefinition} that will be replaced
     * @param editor       {@link Consumer} containing updates to the resource
     */
    @Override
    public void replace(String resourceName, Consumer<CustomResourceDefinition> editor) {
        CustomResourceDefinition toBeUpdated = client.withName(resourceName).get();
        editor.accept(toBeUpdated);
        update(toBeUpdated);
    }

    /**
     * Waits for {@link CustomResourceDefinition} to be ready (created/running)
     *
     * @param resource
     * @return result of the readiness check
     */
    @Override
    public boolean waitForReadiness(CustomResourceDefinition resource) {
        return resource != null;
    }

    /**
     * Waits for {@link CustomResourceDefinition} to be deleted
     *
     * @param resource
     * @return result of the deletion
     */
    @Override
    public boolean waitForDeletion(CustomResourceDefinition resource) {
        return resource == null;
    }
}
