/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.resources;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.skodjob.testframe.interfaces.ThrowableRunner;

/**
 * Represents an item containing a Kubernetes resource and a runnable action.
 * @param <T> Type of Kubernetes resource.
 */
public final class ResourceItem<T extends HasMetadata>  {

    private final ThrowableRunner throwableRunner;
    private final T resource;

    /**
     * Constructs a ResourceItem with the given runnable action and resource.
     * @param throwableRunner The runnable action to execute.
     * @param resource The Kubernetes resource associated with the action.
     */
    public ResourceItem(ThrowableRunner throwableRunner, T resource) {
        this.throwableRunner = throwableRunner;
        this.resource = resource;
    }

    /**
     * Constructs a ResourceItem with the given runnable action.
     * @param throwableRunner The runnable action to execute.
     */
    public ResourceItem(ThrowableRunner throwableRunner) {
        this.throwableRunner = throwableRunner;
        this.resource = null;
    }

    /**
     * Gets the runnable action associated with this resource item.
     * @return The runnable action.
     */
    public ThrowableRunner getThrowableRunner() {
        return throwableRunner;
    }

    /**
     * Gets the Kubernetes resource associated with this resource item.
     * @return The Kubernetes resource.
     */
    public T getResource() {
        return resource;
    }
}
