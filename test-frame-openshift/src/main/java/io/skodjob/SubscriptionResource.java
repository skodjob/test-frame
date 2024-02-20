package io.skodjob;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.Subscription;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.SubscriptionList;
import io.skodjob.clients.KubeClient;
import io.skodjob.interfaces.NamespacedResourceType;

import java.util.function.Consumer;

public class SubscriptionResource implements NamespacedResourceType<Subscription, SubscriptionList, Resource<Subscription>> {

    private final MixedOperation<Subscription, SubscriptionList, Resource<Subscription>> client;

    public SubscriptionResource() {
        this.client = KubeClient.getInstance().getOpenShiftClient().operatorHub().subscriptions();
    }

    /**
     * Returns client for resource {@link Subscription}
     *
     * @return client of a MixedOperation<{@link Subscription}, {@link SubscriptionList}, Resource<{@link Subscription}>> resource
     */
    @Override
    public MixedOperation<Subscription, SubscriptionList, Resource<Subscription>> getClient() {
        return client;
    }

    /**
     * Creates specific {@link Subscription} resource
     *
     * @param resource {@link Subscription} resource
     */
    @Override
    public void create(Subscription resource) {
        client.resource(resource).create();
    }

    /**
     * Updates specific {@link Subscription} resource
     *
     * @param resource {@link Subscription} resource that will be updated
     */
    @Override
    public void update(Subscription resource) {
        client.resource(resource).update();
    }

    /**
     * Deletes {@link Subscription} resource from Namespace in current context
     *
     * @param resourceName name of the {@link Subscription} that will be deleted
     */
    @Override
    public void delete(String resourceName) {
        client.withName(resourceName).delete();
    }

    /**
     * Replaces {@link Subscription} resource using {@link Consumer}
     * from which is the current {@link Subscription} resource updated
     *
     * @param resourceName name of the {@link Subscription} that will be replaced
     * @param editor       {@link Consumer} containing updates to the resource
     */
    @Override
    public void replace(String resourceName, Consumer<Subscription> editor) {
        Subscription toBeReplaced = client.withName(resourceName).get();
        editor.accept(toBeReplaced);
        update(toBeReplaced);
    }

    /**
     * Waits for {@link Subscription} to be ready (created/running)
     *
     * @param resource
     * @return result of the readiness check
     */
    @Override
    public boolean waitForReadiness(Subscription resource) {
        return resource != null;
    }

    /**
     * Waits for {@link Subscription} to be deleted
     *
     * @param resource
     * @return result of the deletion
     */
    @Override
    public boolean waitForDeletion(Subscription resource) {
        return resource == null;
    }

    /**
     * Creates specific {@link Subscription} resource in Namespace specified by user
     *
     * @param namespaceName Namespace, where the resource should be created
     * @param resource      {@link Subscription} resource
     */
    @Override
    public void createInNamespace(String namespaceName, Subscription resource) {
        client.inNamespace(namespaceName).resource(resource).create();
    }

    /**
     * Updates specific {@link Subscription} resource in Namespace specified by user
     *
     * @param namespaceName Namespace, where the resource should be updated
     * @param resource      {@link Subscription} updated resource
     */
    @Override
    public void updateInNamespace(String namespaceName, Subscription resource) {
        client.inNamespace(namespaceName).resource(resource).update();
    }

    /**
     * Deletes {@link Subscription} resource from Namespace specified by user
     *
     * @param namespaceName Namespace, where the resource should be deleted
     * @param resourceName  name of the {@link Subscription} that will be deleted
     */
    @Override
    public void deleteFromNamespace(String namespaceName, String resourceName) {
        client.inNamespace(namespaceName).withName(resourceName).delete();
    }

    /**
     * Replaces {@link Subscription} resource in Namespace specified by user, using {@link Consumer}
     * from which is the current {@link Subscription} resource updated
     *
     * @param namespaceName Namespace, where the resource should be replaced
     * @param resourceName  name of the {@link Subscription} that will be replaced
     * @param editor        {@link Consumer} containing updates to the resource
     */
    @Override
    public void replaceInNamespace(String namespaceName, String resourceName, Consumer<Subscription> editor) {
        Subscription toBeReplaced = client.inNamespace(namespaceName).withName(resourceName).get();
        editor.accept(toBeReplaced);
        updateInNamespace(namespaceName, toBeReplaced);
    }
}
