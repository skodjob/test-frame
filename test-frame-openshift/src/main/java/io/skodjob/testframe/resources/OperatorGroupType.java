/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.resources;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.api.model.operatorhub.v1.OperatorGroup;
import io.fabric8.openshift.api.model.operatorhub.v1.OperatorGroupList;
import io.skodjob.testframe.interfaces.NamespacedResourceType;

import java.util.function.Consumer;

/**
 * Implementation of ResourceType for specific kubernetes resource
 */
public class OperatorGroupType implements NamespacedResourceType<OperatorGroup> {

    private final MixedOperation<OperatorGroup, OperatorGroupList, Resource<OperatorGroup>> client;

    /**
     * Constructor
     */
    public OperatorGroupType() {
        this.client = KubeResourceManager.getKubeClient().getOpenShiftClient().operatorHub().operatorGroups();
    }

    /**
     * Kind of api resource
     *
     * @return kind name
     */
    @Override
    public String getKind() {
        return "OperatorGroup";
    }

    /**
     * Get specific client for resoruce
     *
     * @return specific client
     */
    @Override
    public MixedOperation<?, ?, ?> getClient() {
        return client;
    }

    /**
     * Creates specific {@link OperatorGroup} resource
     *
     * @param resource {@link OperatorGroup} resource
     */
    @Override
    public void create(OperatorGroup resource) {
        client.resource(resource).create();
    }

    /**
     * Updates specific {@link OperatorGroup} resource
     *
     * @param resource {@link OperatorGroup} resource that will be updated
     */
    @Override
    public void update(OperatorGroup resource) {
        client.resource(resource).update();
    }

    /**
     * Deletes {@link OperatorGroup} resource from Namespace in current context
     *
     * @param resourceName name of the {@link OperatorGroup} that will be deleted
     */
    @Override
    public void delete(String resourceName) {
        client.withName(resourceName).delete();
    }

    /**
     * Replaces {@link OperatorGroup} resource using {@link Consumer}
     * from which is the current {@link OperatorGroup} resource updated
     *
     * @param resourceName name of the {@link OperatorGroup} that will be replaced
     * @param editor       {@link Consumer} containing updates to the resource
     */
    @Override
    public void replace(String resourceName, Consumer<OperatorGroup> editor) {
        OperatorGroup toBeReplaced = client.withName(resourceName).get();
        editor.accept(toBeReplaced);
        update(toBeReplaced);
    }

    /**
     * Waits for {@link OperatorGroup} to be ready (created/running)
     *
     * @param resource resource
     * @return result of the readiness check
     */
    @Override
    public boolean isReady(OperatorGroup resource) {
        return resource != null;
    }

    /**
     * Waits for {@link OperatorGroup} to be deleted
     *
     * @param resource resource
     * @return result of the deletion
     */
    @Override
    public boolean isDeleted(OperatorGroup resource) {
        return resource == null;
    }

    /**
     * Creates specific {@link OperatorGroup} resource in Namespace specified by user
     *
     * @param namespaceName Namespace, where the resource should be created
     * @param resource      {@link OperatorGroup} resource
     */
    @Override
    public void createInNamespace(String namespaceName, OperatorGroup resource) {
        client.inNamespace(namespaceName).resource(resource).create();
    }

    /**
     * Updates specific {@link OperatorGroup} resource in Namespace specified by user
     *
     * @param namespaceName Namespace, where the resource should be updated
     * @param resource      {@link OperatorGroup} updated resource
     */
    @Override
    public void updateInNamespace(String namespaceName, OperatorGroup resource) {
        client.inNamespace(namespaceName).resource(resource).update();
    }

    /**
     * Deletes {@link OperatorGroup} resource from Namespace specified by user
     *
     * @param namespaceName Namespace, where the resource should be deleted
     * @param resourceName  name of the {@link OperatorGroup} that will be deleted
     */
    @Override
    public void deleteFromNamespace(String namespaceName, String resourceName) {
        client.inNamespace(namespaceName).withName(resourceName).delete();
    }

    /**
     * Replaces {@link OperatorGroup} resource in Namespace specified by user, using {@link Consumer}
     * from which is the current {@link OperatorGroup} resource updated
     *
     * @param namespaceName Namespace, where the resource should be replaced
     * @param resourceName  name of the {@link OperatorGroup} that will be replaced
     * @param editor        {@link Consumer} containing updates to the resource
     */
    @Override
    public void replaceInNamespace(String namespaceName, String resourceName, Consumer<OperatorGroup> editor) {
        OperatorGroup toBeReplaced = client.inNamespace(namespaceName).withName(resourceName).get();
        editor.accept(toBeReplaced);
        updateInNamespace(namespaceName, toBeReplaced);
    }
}
