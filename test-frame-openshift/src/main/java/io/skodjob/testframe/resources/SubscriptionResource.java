/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.resources;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.Subscription;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.SubscriptionList;
import io.skodjob.testframe.interfaces.NamespacedResourceType;

import java.util.function.Consumer;

public class SubscriptionResource implements NamespacedResourceType<Subscription> {

    private final MixedOperation<Subscription, SubscriptionList, Resource<Subscription>> client;

    public SubscriptionResource() {
        this.client = ResourceManager.getKubeClient().getOpenShiftClient().operatorHub().subscriptions();
    }

    /**
     * Kind of api resource
     * @return kind name
     */
    @Override
    public String getKind() {
        return "Subscription";
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
