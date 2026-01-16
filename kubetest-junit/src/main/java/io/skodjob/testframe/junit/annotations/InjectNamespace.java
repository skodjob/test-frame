/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.junit.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to inject the current test namespace.
 * This can inject either the namespace name as a String or the Namespace object itself.
 *
 * Usage:
 * <pre>
 * &#64;KubernetesTest(namespace = "my-test")
 * class MyTest {
 *     &#64;InjectNamespace
 *     String namespaceName; // Will be "my-test"
 *
 *     &#64;InjectNamespace
 *     Namespace namespace;  // The actual Namespace object
 *
 *     &#64;Test
 *     void testInNamespace() {
 *         assertEquals("my-test", namespaceName);
 *         assertNotNull(namespace);
 *     }
 * }
 * </pre>
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface InjectNamespace {
    // No additional properties needed
}