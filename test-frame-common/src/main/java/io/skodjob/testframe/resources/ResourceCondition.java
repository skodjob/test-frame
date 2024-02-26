/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.skodjob.testframe.resources;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.interfaces.ResourceType;

import java.util.Objects;
import java.util.function.Predicate;

public class ResourceCondition<T extends HasMetadata> {
    private final Predicate<T> predicate;
    private final String conditionName;

    public ResourceCondition(Predicate<T> predicate, String conditionName) {
        this.predicate = predicate;
        this.conditionName = conditionName;
    }

    public String getConditionName() {
        return conditionName;
    }

    public Predicate<T> getPredicate() {
        return predicate;
    }

    public static <T extends HasMetadata> ResourceCondition<T> readiness(ResourceType<T> type) {
        return new ResourceCondition<>(type::waitForReadiness, "readiness");
    }

    public static <T extends HasMetadata> ResourceCondition<T> deletion() {
        return new ResourceCondition<>(Objects::isNull, "deletion");
    }
}
