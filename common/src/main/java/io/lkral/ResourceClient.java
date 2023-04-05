package io.lkral;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

/**
 * Class for encapsulating methods related to {@code T} resource.
 * @param <T> resource type
 * @param <L> resource's list
 */
public interface ResourceClient<T extends HasMetadata, L extends KubernetesResourceList<T>> {

    /**
     * Returns client for resource {@code T}
     * @return client of a MixedOperation<{@code T}, {@code L}, Resource<{@code T}>> resource
     */
    MixedOperation<T, L, Resource<T>> getClient();

    /**
     * Creates specific {@code T} resource
     * @param resource {@code T} resource
     */
    void create(T resource);

    /**
     * Creates specific {@code T} resource in specified namespace
     * @param namespaceName name of Namespace, where the {@code T} should be created
     * @param resource {@code T} resource
     */
    void createInNamespace(String namespaceName, T resource);

    /**
     * Gets {@code T} resource from Namespace in current context
     * @param resourceName name of the {@code T} resource
     * @return resource {@code T}
     */
    T get(String resourceName);

    /**
     * Gets {@code T} resource from specified Namespace
     * @param namespaceName name of Namespace, from where the {@code T} should be obtained
     * @param resourceName name of the {@code T} resource
     * @return desired {@code T} resource
     */
    T getFromNamespace(String namespaceName, String resourceName);

    /**
     * Updates specific {@code T} resource
     * @param resource {@code T} resource that will be updated
     */
    void update(T resource);

    /**
     * Updates specific {@code T} resource in specified Namespace
     * @param namespaceName name of Namespace, where the {@code T} should be updated
     * @param resource {@code T} resource that will be updated
     */
    void updateInNamespace(String namespaceName, T resource);

    /**
     * Deletes {@code T} resource from Namespace in current context
     * @param resourceName name of the {@code T} that will be deleted
     */
    void delete(String resourceName);

    /**
     * Deletes {@code T} resource from specified Namespace
     * @param namespaceName name of Namespace, from where the {@code T} will be deleted
     * @param resourceName name of the {@code T} resource
     */
    void deleteFromNamespace(String namespaceName, String resourceName);
}
