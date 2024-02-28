/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.interfaces;

import java.util.function.Consumer;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.dsl.MixedOperation;

public interface NamespacedResourceType<T extends HasMetadata> extends ResourceType<T> {

    /**
     * Get specific {@link T} client for resoruce
     * @return specific client
     */
    MixedOperation<?, ?, ?> getClient();

    /**
     * Creates specific {@link T} resource in Namespace specified by user
     * @param namespaceName Namespace, where the resource should be created
     * @param resource {@link T} resource
     */
    void createInNamespace(String namespaceName, T resource);

    /**
     * Updates specific {@link T} resource in Namespace specified by user
     * @param namespaceName Namespace, where the resource should be updated
     * @param resource {@link T} updated resource
     */
    void updateInNamespace(String namespaceName, T resource);

    /**
     * Deletes {@link T} resource from Namespace specified by user
     * @param namespaceName Namespace, where the resource should be deleted
     * @param resourceName name of the {@link T} that will be deleted
     */
    void deleteFromNamespace(String namespaceName, String resourceName);

    /**
     * Replaces {@link T} resource in Namespace specified by user, using {@link Consumer}
     * from which is the current {@link T} resource updated
     * @param namespaceName Namespace, where the resource should be replaced
     * @param resourceName name of the {@link T} that will be replaced
     * @param editor {@link Consumer} containing updates to the resource
     */
    void replaceInNamespace(String namespaceName, String resourceName, Consumer<T> editor);
}
