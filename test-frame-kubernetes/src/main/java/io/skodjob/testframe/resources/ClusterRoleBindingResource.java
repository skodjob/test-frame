package io.skodjob.testframe.resources;

import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBindingList;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.clients.KubeClient;
import io.skodjob.testframe.interfaces.ResourceType;

import java.util.function.Consumer;

public class ClusterRoleBindingResource implements ResourceType<ClusterRoleBinding, ClusterRoleBindingList, Resource<ClusterRoleBinding>> {

    private final NonNamespaceOperation<ClusterRoleBinding, ClusterRoleBindingList, Resource<ClusterRoleBinding>> client;

    public ClusterRoleBindingResource() {
        this.client = KubeClient.getInstance().getClient().rbac().clusterRoleBindings();
    }

    /**
     * Returns client for resource {@link ClusterRoleBinding}
     *
     * @return client of a MixedOperation<{@link ClusterRoleBinding}, {@link ClusterRoleBindingList}, Resource<{@link ClusterRoleBinding}>> resource
     */
    @Override
    public NonNamespaceOperation<ClusterRoleBinding, ClusterRoleBindingList, Resource<ClusterRoleBinding>> getClient() {
        return client;
    }

    /**
     * Creates specific {@link ClusterRoleBinding} resource
     *
     * @param resource {@link ClusterRoleBinding} resource
     */
    @Override
    public void create(ClusterRoleBinding resource) {
        client.resource(resource).create();
    }

    /**
     * Updates specific {@link ClusterRoleBinding} resource
     *
     * @param resource {@link ClusterRoleBinding} resource that will be updated
     */
    @Override
    public void update(ClusterRoleBinding resource) {
        client.resource(resource).update();
    }

    /**
     * Deletes {@link ClusterRoleBinding} resource from Namespace in current context
     *
     * @param resourceName name of the {@link ClusterRoleBinding} that will be deleted
     */
    @Override
    public void delete(String resourceName) {
        client.withName(resourceName).delete();
    }

    /**
     * Replaces {@link ClusterRoleBinding} resource using {@link Consumer}
     * from which is the current {@link ClusterRoleBinding} resource updated
     *
     * @param resourceName name of the {@link ClusterRoleBinding} that will be replaced
     * @param editor       {@link Consumer} containing updates to the resource
     */
    @Override
    public void replace(String resourceName, Consumer<ClusterRoleBinding> editor) {
        ClusterRoleBinding toBeUpdated = client.withName(resourceName).get();
        editor.accept(toBeUpdated);
        update(toBeUpdated);
    }

    /**
     * Waits for {@link ClusterRoleBinding} to be ready (created/running)
     *
     * @param resource
     * @return result of the readiness check
     */
    @Override
    public boolean waitForReadiness(ClusterRoleBinding resource) {
        return resource != null;
    }

    /**
     * Waits for {@link ClusterRoleBinding} to be deleted
     *
     * @param resource
     * @return result of the deletion
     */
    @Override
    public boolean waitForDeletion(ClusterRoleBinding resource) {
        return resource == null;
    }
}
