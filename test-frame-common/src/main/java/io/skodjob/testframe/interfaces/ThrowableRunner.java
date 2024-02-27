/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.interfaces;

@FunctionalInterface
public interface ThrowableRunner {
    void run() throws Exception;
}
