package io.skodjob.testframe.interfaces;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import java.util.function.Consumer;

public interface NamespacedResourceType<T extends HasMetadata, L extends KubernetesResourceList<T>, M extends Resource<T>> extends ResourceType<T, L, M> {

    /**
     * Returns client for resource {@link T}
     * @return client of a MixedOperation<{@link T}, {@link L}, Resource<{@link T}>> resource
     */
    MixedOperation<T, L, M> getClient();

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
