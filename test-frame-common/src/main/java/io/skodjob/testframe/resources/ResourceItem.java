/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.resources;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.skodjob.testframe.interfaces.ThrowableRunner;

public final class ResourceItem<T extends HasMetadata>  {

    ThrowableRunner throwableRunner;
    T resource;

    public ResourceItem(ThrowableRunner throwableRunner, T resource) {
        this.throwableRunner = throwableRunner;
        this.resource = resource;
    }

    public ResourceItem(ThrowableRunner throwableRunner) {
        this.throwableRunner = throwableRunner;
    }

    public ThrowableRunner getThrowableRunner() {
        return throwableRunner;
    }
    public T getResource() {
        return resource;
    }
}
