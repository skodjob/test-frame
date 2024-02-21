package io.skodjob.testframe.resources;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.clients.KubeClient;
import io.skodjob.testframe.interfaces.NamespacedResourceType;

import java.util.function.Consumer;

public class SecretResource implements NamespacedResourceType<Secret, SecretList, Resource<Secret>> {

    private final MixedOperation<Secret, SecretList, Resource<Secret>> client;

    public SecretResource() {
        this.client = KubeClient.getInstance().getClient().secrets();
    }

    /**
     * Returns client for resource {@link Secret}
     *
     * @return client of a MixedOperation<{@link Secret}, {@link SecretList}, Resource<{@link Secret}>> resource
     */
    @Override
    public MixedOperation<Secret, SecretList, Resource<Secret>> getClient() {
        return client;
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
     * @param resourceName
     * @param editor
     */
    @Override
    public void replace(String resourceName, Consumer<Secret> editor) {
        Secret toBeReplaced = client.withName(resourceName).get();
        editor.accept(toBeReplaced);
        update(toBeReplaced);
    }

    /**
     * Waits for {@link T} to be ready (created/running)
     *
     * @param resource
     * @return result of the readiness check
     */
    @Override
    public boolean waitForReadiness(Secret resource) {
        return false;
    }

    /**
     * Waits for {@link T} to be deleted
     *
     * @param resource
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
     * @param namespaceName
     * @param resourceName
     * @param editor
     */
    @Override
    public void replaceInNamespace(String namespaceName, String resourceName, Consumer<Secret> editor) {
        Secret toBeReplaced = client.inNamespace(namespaceName).withName(resourceName).get();
        editor.accept(toBeReplaced);
        updateInNamespace(namespaceName, toBeReplaced);
    }
}
