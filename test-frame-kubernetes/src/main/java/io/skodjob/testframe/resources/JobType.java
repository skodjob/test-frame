/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.resources;

import java.util.function.Consumer;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.ScalableResource;
import io.skodjob.testframe.interfaces.NamespacedResourceType;

/**
 * Implementation of ResourceType for specific kubernetes resource
 */
public class JobType implements NamespacedResourceType<Job> {

    private final MixedOperation<Job, JobList, ScalableResource<Job>> client;

    /**
     * Constructor
     */
    public JobType() {
        this.client = KubeResourceManager.getKubeClient().getClient().batch().v1().jobs();
    }

    /**
     * Kind of api resource
     *
     * @return kind name
     */
    @Override
    public String getKind() {
        return "Job";
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
     * Creates specific {@link Job} resource in Namespace specified by user
     *
     * @param namespaceName Namespace, where the resource should be created
     * @param resource      {@link Job} resource
     */
    @Override
    public void createInNamespace(String namespaceName, Job resource) {
        client.inNamespace(namespaceName).resource(resource).create();
    }

    /**
     * Updates specific {@link Job} resource in Namespace specified by user
     *
     * @param namespaceName Namespace, where the resource should be updated
     * @param resource      {@link Job} updated resource
     */
    @Override
    public void updateInNamespace(String namespaceName, Job resource) {
        client.inNamespace(namespaceName).resource(resource).update();
    }

    /**
     * Deletes {@link Job} resource from Namespace specified by user
     *
     * @param namespaceName Namespace, where the resource should be deleted
     * @param resourceName  name of the {@link Job} that will be deleted
     */
    @Override
    public void deleteFromNamespace(String namespaceName, String resourceName) {
        client.inNamespace(namespaceName).withName(resourceName).delete();
    }

    /**
     * Replaces {@link Job} resource in Namespace specified by user, using {@link Consumer}
     * from which is the current {@link Job} resource updated
     *
     * @param namespaceName Namespace, where the resource should be replaced
     * @param resourceName  name of the {@link Job} that will be replaced
     * @param editor        {@link Consumer} containing updates to the resource
     */
    @Override
    public void replaceInNamespace(String namespaceName, String resourceName, Consumer<Job> editor) {
        Job toBeUpdated = client.inNamespace(namespaceName).withName(resourceName).get();
        editor.accept(toBeUpdated);
        updateInNamespace(namespaceName, toBeUpdated);
    }

    /**
     * Creates specific {@link Job} resource
     *
     * @param resource {@link Job} resource
     */
    @Override
    public void create(Job resource) {
        client.resource(resource).create();
    }

    /**
     * Updates specific {@link Job} resource
     *
     * @param resource {@link Job} resource that will be updated
     */
    @Override
    public void update(Job resource) {
        client.resource(resource).update();
    }

    /**
     * Deletes {@link Job} resource from Namespace in current context
     *
     * @param resourceName name of the {@link Job} that will be deleted
     */
    @Override
    public void delete(String resourceName) {
        client.withName(resourceName).delete();
    }

    /**
     * Replaces {@link Job} resource using {@link Consumer}
     * from which is the current {@link Job} resource updated
     *
     * @param resourceName name of the {@link Job} that will be replaced
     * @param editor       {@link Consumer} containing updates to the resource
     */
    @Override
    public void replace(String resourceName, Consumer<Job> editor) {
        Job toBeUpdated = client.withName(resourceName).get();
        editor.accept(toBeUpdated);
        update(toBeUpdated);
    }

    /**
     * Waits for {@link Job} to be ready (created/running)
     *
     * @param resource resource
     * @return result of the readiness check
     */
    @Override
    public boolean waitForReadiness(Job resource) {
        return client.resource(resource).isReady();
    }

    /**
     * Waits for {@link Job} to be deleted
     *
     * @param resource resource
     * @return result of the deletion
     */
    @Override
    public boolean waitForDeletion(Job resource) {
        return resource == null;
    }
}
