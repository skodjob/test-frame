/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.interfaces;

import io.fabric8.kubernetes.api.model.HasMetadata;

import java.util.function.Consumer;

/**
 * Class for encapsulating methods related to {@link T} resource.
 * @param <T> resource type
 */
public interface ResourceType<T extends HasMetadata> {
    /**
     * Kind of api resource
     * @return kind name
     */
    String getKind();

    /**
     * Creates specific {@link T} resource
     * @param resource {@link T} resource
     */
    void create(T resource);

    /**
     * Updates specific {@link T} resource
     * @param resource {@link T} resource that will be updated
     */
    void update(T resource);

    /**
     * Deletes {@link T} resource from Namespace in current context
     * @param resourceName name of the {@link T} that will be deleted
     */
    void delete(String resourceName);

    /**
     * Replaces {@link T} resource using {@link Consumer}
     * from which is the current {@link T} resource updated
     * @param resourceName name of the {@link T} that will be replaced
     * @param editor {@link Consumer} containing updates to the resource
     */
    void replace(String resourceName, Consumer<T> editor);

    /**
     * Waits for {@link T} to be ready (created/running)
     * @return result of the readiness check
     */
    boolean waitForReadiness(T resource);

    /**
     * Waits for {@link T} to be deleted
     * @return result of the deletion
     */
    boolean waitForDeletion(T resource);
}
