/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.listeners;

import io.skodjob.testframe.LogCollector;
import io.skodjob.testframe.annotations.MustGather;
import io.skodjob.testframe.interfaces.ThrowableRunner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

/**
 * Represents global log collector which is automatically called on text fail or error even in setup or post methods
 */
public class MustGatherController implements TestExecutionExceptionHandler, LifecycleMethodExecutionExceptionHandler {
    private static final Logger LOGGER = LogManager.getLogger(MustGatherController.class);
    private static LogCollector mustGatherInstance;
    private static ThrowableRunner collectCallback;

    /**
     * Private constructor
     */
    private MustGatherController() {
        // empty constructor
    }

    /**
     * Handler when test fails
     *
     * @param extensionContext extension context
     * @param throwable        throwable
     * @throws Throwable original throwable
     */
    @Override
    public void handleTestExecutionException(ExtensionContext extensionContext, Throwable throwable) throws Throwable {
        saveKubeState(extensionContext);
        throw throwable;
    }

    /**
     * Handles beforeAll exception
     *
     * @param context extensionContext
     * @param throwable throwable
     * @throws Throwable original throwable
     */
    @Override
    public void handleBeforeAllMethodExecutionException(ExtensionContext context, Throwable throwable)throws Throwable {
        saveKubeState(context);
        LifecycleMethodExecutionExceptionHandler.super.handleBeforeAllMethodExecutionException(context, throwable);
    }

    /**
     * Handles beforeEach exception
     *
     * @param context extensionContext
     * @param throwable throwable
     * @throws Throwable original throwable
     */
    @Override
    public void handleBeforeEachMethodExecutionException(ExtensionContext context,
                                                         Throwable throwable) throws Throwable {
        saveKubeState(context);
        LifecycleMethodExecutionExceptionHandler.super.handleBeforeEachMethodExecutionException(context, throwable);
    }

    /**
     * Handles afterEach exception
     *
     * @param context extensionContext
     * @param throwable throwable
     * @throws Throwable original throwable
     */
    @Override
    public void handleAfterEachMethodExecutionException(ExtensionContext context,
                                                        Throwable throwable) throws Throwable {
        saveKubeState(context);
        LifecycleMethodExecutionExceptionHandler.super.handleAfterEachMethodExecutionException(context, throwable);
    }

    /**
     * Handles afterAll exception
     *
     * @param context extensionContext
     * @param throwable throwable
     * @throws Throwable original throwable
     */
    @Override
    public void handleAfterAllMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        saveKubeState(context);
        LifecycleMethodExecutionExceptionHandler.super.handleAfterAllMethodExecutionException(context, throwable);
    }

    /**
     * Setup globalLogCollector which is automatically used within {@link MustGather} annotation
     *
     * @param globalLogCollector log collector instance
     */
    public static void setupMustGatherController(LogCollector globalLogCollector) {
        mustGatherInstance = globalLogCollector;
    }

    /**
     * Returns globalLogCollector instance
     *
     * @return global log collector instance
     */
    public static LogCollector getMustGatherController() {
        if (mustGatherInstance == null) {
            throw new NullPointerException("Global log collector is not initialized");
        }
        return mustGatherInstance;
    }

    /**
     * Adds callback for running log collecting
     *
     * @param callback callback method with log collecting
     */
    public static void setMustGatherCallback(ThrowableRunner callback) {
        collectCallback = callback;
    }

    private void saveKubeState(ExtensionContext context) {
        try {
            if (collectCallback != null) {
                collectCallback.run();
            } else {
                LOGGER.warn("No logCallback defined");
            }
        } catch (Exception ex) {
            LOGGER.error("Cannot collect all data", ex);
        }
    }
}
