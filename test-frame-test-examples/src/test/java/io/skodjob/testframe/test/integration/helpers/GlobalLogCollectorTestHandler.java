/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.test.integration.helpers;

import io.skodjob.testframe.test.integration.AbstractIT;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Override test failure for expected test issue in GlobalLogCollectorIT test
 */
public class GlobalLogCollectorTestHandler implements TestExecutionExceptionHandler {

    /**
     * Check cause message and if it is from GlobalLogCollectorIT check if logs
     * are collected and do not mark test as failure
     *
     * @param context extension context
     * @param cause   throwable object
     * @throws Throwable throwable object
     */
    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable cause) throws Throwable {
        if (cause.getMessage().contains("Expected issue")) {
            assertTrue(AbstractIT.LOG_DIR.toFile().exists());
            assertTrue(AbstractIT.LOG_DIR.resolve("default").toFile().exists());
            assertTrue(AbstractIT.LOG_DIR.resolve("cluster-wide-resources").toFile().exists());
        } else {
            throw cause;
        }
    }
}
