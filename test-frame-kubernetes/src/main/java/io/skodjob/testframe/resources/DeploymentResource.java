package io.skodjob.testframe.resources;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import io.skodjob.testframe.clients.KubeClient;
import io.skodjob.testframe.interfaces.NamespacedResourceType;

import java.util.function.Consumer;

public class DeploymentResource implements NamespacedResourceType<Deployment, DeploymentList, RollableScalableResource<Deployment>> {

    private final MixedOperation<Deployment, DeploymentList, RollableScalableResource<Deployment>> client;

    public DeploymentResource() {
        this.client = KubeClient.getInstance().getClient().apps().deployments();
    }

    /**
     * Returns client for resource {@link Deployment}
     *
     * @return client of a MixedOperation<{@link Deployment}, {@link L}, Resource<{@link Deployment}>> resource
     */
    @Override
    public MixedOperation<Deployment, DeploymentList, RollableScalableResource<Deployment>> getClient() {
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
     * @param resource
     * @return result of the readiness check
     */
    @Override
    public boolean waitForReadiness(Deployment resource) {
        return client.resource(resource).isReady();
    }

    /**
     * Waits for {@link Deployment} to be deleted
     *
     * @param resource
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
