/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.resources;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.InstallPlan;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.InstallPlanList;
import io.skodjob.testframe.interfaces.NamespacedResourceType;

import java.util.function.Consumer;

/**
 * Implementation of ResourceType for specific kubernetes resource
 */
public class InstallPlanType implements NamespacedResourceType<InstallPlan> {

    private final MixedOperation<InstallPlan, InstallPlanList, Resource<InstallPlan>> client;

    /**
     * Constructor
     */
    public InstallPlanType() {
        this.client = KubeResourceManager.getKubeClient().getOpenShiftClient().operatorHub().installPlans();
    }

    /**
     * Kind of api resource
     * @return kind name
     */
    @Override
    public String getKind() {
        return "OperatorGroup";
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
     * Creates specific {@link InstallPlan} resource
     *
     * @param resource {@link InstallPlan} resource
     */
    @Override
    public void create(InstallPlan resource) {
        client.resource(resource).create();
    }

    /**
     * Updates specific {@link InstallPlan} resource
     *
     * @param resource {@link InstallPlan} resource that will be updated
     */
    @Override
    public void update(InstallPlan resource) {
        client.resource(resource).update();
    }

    /**
     * Deletes {@link InstallPlan} resource from Namespace in current context
     *
     * @param resourceName name of the {@link InstallPlan} that will be deleted
     */
    @Override
    public void delete(String resourceName) {
        client.withName(resourceName).delete();
    }

    /**
     * Replaces {@link InstallPlan} resource using {@link Consumer}
     * from which is the current {@link InstallPlan} resource updated
     *
     * @param resourceName name of the {@link InstallPlan} that will be replaced
     * @param editor       {@link Consumer} containing updates to the resource
     */
    @Override
    public void replace(String resourceName, Consumer<InstallPlan> editor) {
        InstallPlan toBeReplaced = client.withName(resourceName).get();
        editor.accept(toBeReplaced);
        update(toBeReplaced);
    }

    /**
     * Waits for {@link InstallPlan} to be ready (created/running)
     *
     * @param resource resource
     * @return result of the readiness check
     */
    @Override
    public boolean waitForReadiness(InstallPlan resource) {
        return resource != null;
    }

    /**
     * Waits for {@link InstallPlan} to be deleted
     *
     * @param resource resource
     * @return result of the deletion
     */
    @Override
    public boolean waitForDeletion(InstallPlan resource) {
        return resource == null;
    }

    /**
     * Creates specific {@link InstallPlan} resource in Namespace specified by user
     *
     * @param namespaceName Namespace, where the resource should be created
     * @param resource      {@link InstallPlan} resource
     */
    @Override
    public void createInNamespace(String namespaceName, InstallPlan resource) {
        client.inNamespace(namespaceName).resource(resource).create();
    }

    /**
     * Updates specific {@link InstallPlan} resource in Namespace specified by user
     *
     * @param namespaceName Namespace, where the resource should be updated
     * @param resource      {@link InstallPlan} updated resource
     */
    @Override
    public void updateInNamespace(String namespaceName, InstallPlan resource) {
        client.inNamespace(namespaceName).resource(resource).update();
    }

    /**
     * Deletes {@link InstallPlan} resource from Namespace specified by user
     *
     * @param namespaceName Namespace, where the resource should be deleted
     * @param resourceName  name of the {@link InstallPlan} that will be deleted
     */
    @Override
    public void deleteFromNamespace(String namespaceName, String resourceName) {
        client.inNamespace(namespaceName).withName(resourceName).delete();
    }

    /**
     * Replaces {@link InstallPlan} resource in Namespace specified by user, using {@link Consumer}
     * from which is the current {@link InstallPlan} resource updated
     *
     * @param namespaceName Namespace, where the resource should be replaced
     * @param resourceName  name of the {@link InstallPlan} that will be replaced
     * @param editor        {@link Consumer} containing updates to the resource
     */
    @Override
    public void replaceInNamespace(String namespaceName, String resourceName, Consumer<InstallPlan> editor) {
        InstallPlan toBeReplaced = client.inNamespace(namespaceName).withName(resourceName).get();
        editor.accept(toBeReplaced);
        updateInNamespace(namespaceName, toBeReplaced);
    }
}
