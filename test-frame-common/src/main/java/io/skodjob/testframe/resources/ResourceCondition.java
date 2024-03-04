/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.resources;

import java.util.Objects;
import java.util.function.Predicate;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.skodjob.testframe.interfaces.ResourceType;

/**
 * Represents a condition that can be applied to Kubernetes resources.
 *
 * @param <T> Type of Kubernetes resource.
 */
public class ResourceCondition<T extends HasMetadata> {
    private final Predicate<T> predicate;
    private final String conditionName;

    /**
     * Constructs a ResourceCondition with the given predicate and condition name.
     *
     * @param predicate     The predicate representing the condition.
     * @param conditionName The name of the condition.
     */
    public ResourceCondition(Predicate<T> predicate, String conditionName) {
        this.predicate = predicate;
        this.conditionName = conditionName;
    }

    /**
     * Gets the name of the condition.
     *
     * @return The name of the condition.
     */
    public String getConditionName() {
        return conditionName;
    }

    /**
     * Gets the predicate representing the condition.
     *
     * @return The predicate representing the condition.
     */
    public Predicate<T> getPredicate() {
        return predicate;
    }

    /**
     * Creates a ResourceCondition representing readiness of a resource of the given type.
     *
     * @param <T>  Type of Kubernetes resource.
     * @param type The resource type.
     * @return The ResourceCondition representing readiness.
     */
    public static <T extends HasMetadata> ResourceCondition<T> readiness(ResourceType<T> type) {
        return new ResourceCondition<>(type::waitForReadiness, "readiness");
    }

    /**
     * Creates a ResourceCondition representing deletion of a resource.
     *
     * @param <T> Type of Kubernetes resource.
     * @return The ResourceCondition representing deletion.
     */
    public static <T extends HasMetadata> ResourceCondition<T> deletion() {
        return new ResourceCondition<>(Objects::isNull, "deletion");
    }
}
