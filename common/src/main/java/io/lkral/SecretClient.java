package io.lkral;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

public class SecretClient implements ResourceClient<Secret, SecretList> {

    private final MixedOperation<Secret, SecretList, Resource<Secret>> secretClient;
    private static SecretClient client;

    private SecretClient() {
        this.secretClient = KubeClient.getInstance().getClient().secrets();
    }

    public static SecretClient client() {
        if (client == null) {
            client = new SecretClient();
        }

        return client;
    }

    /**
     * Returns client for resource {@code Secret}
     *
     * @return client of a MixedOperation<{@code Secret}, {@code SecretList}, Resource<{@code Secret}>> resource
     */
    @Override
    public MixedOperation<Secret, SecretList, Resource<Secret>> getClient() {
        return secretClient;
    }

    /**
     * Creates specific {@code Secret} resource
     *
     * @param resource {@code Secret} resource
     */
    @Override
    public void create(Secret resource) {
        secretClient.resource(resource).create();
    }

    /**
     * Creates specific {@code Secret} resource in specified namespace
     *
     * @param namespaceName name of Namespace, where the {@code Secret} should be created
     * @param resource      {@code Secret} resource
     */
    @Override
    public void createInNamespace(String namespaceName, Secret resource) {
        secretClient.inNamespace(namespaceName).resource(resource).create();
    }

    /**
     * Gets {@code Secret} resource from Namespace in current context
     *
     * @param resourceName name of the {@code Secret} resource
     * @return resource {@code Secret}
     */
    @Override
    public Secret get(String resourceName) {
        return secretClient.withName(resourceName).get();
    }

    /**
     * Gets {@code Secret} resource from specified Namespace
     *
     * @param namespaceName name of Namespace, from where the {@code Secret} should be obtained
     * @param resourceName  name of the {@code Secret} resource
     * @return desired {@code Secret} resource
     */
    @Override
    public Secret getFromNamespace(String namespaceName, String resourceName) {
        return secretClient.inNamespace(namespaceName).withName(resourceName).get();
    }

    /**
     * Updates specific {@code Secret} resource
     *
     * @param resource {@code Secret} resource that will be updated
     */
    @Override
    public void update(Secret resource) {
        secretClient.resource(resource).update();
    }

    /**
     * Updates specific {@code Secret} resource in specified Namespace
     *
     * @param namespaceName name of Namespace, where the {@code Secret} should be updated
     * @param resource      {@code Secret} resource that will be updated
     */
    @Override
    public void updateInNamespace(String namespaceName, Secret resource) {
        secretClient.inNamespace(namespaceName).resource(resource).update();
    }

    /**
     * Deletes {@code Secret} resource from Namespace in current context
     *
     * @param resourceName name of the {@code Secret} that will be deleted
     */
    @Override
    public void delete(String resourceName) {
        secretClient.withName(resourceName).delete();
    }

    /**
     * Deletes {@code Secret} resource from specified Namespace
     *
     * @param namespaceName name of Namespace, from where the {@code Secret} will be deleted
     * @param resourceName  name of the {@code Secret} resource
     */
    @Override
    public void deleteFromNamespace(String namespaceName, String resourceName) {
        secretClient.inNamespace(namespaceName).withName(resourceName).delete();
    }
}
