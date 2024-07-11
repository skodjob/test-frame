/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.annotations;

import io.skodjob.testframe.listeners.MustGatherController;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * This annotation is used to automatically call log collecting
 * when test of prepare and post phase fails in JUnit tests.
 * It is applied at the class level.
 * <p>
 * It uses the {@link MustGatherController}
 */
@Target(ElementType.TYPE)
@Retention(RUNTIME)
@Inherited
@ExtendWith(MustGatherController.class)
public @interface MustGather {
}
