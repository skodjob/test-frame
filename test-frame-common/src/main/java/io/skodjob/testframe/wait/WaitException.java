/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.wait;

public class WaitException extends RuntimeException {
    public WaitException(String message) {
        super(message);
    }
}