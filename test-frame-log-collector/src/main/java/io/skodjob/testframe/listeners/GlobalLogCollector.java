/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.listeners;

import io.skodjob.testframe.LogCollector;
import io.skodjob.testframe.annotations.CollectLogs;
import io.skodjob.testframe.interfaces.ThrowableRunner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

import java.util.LinkedList;
import java.util.List;

/**
 * Represents global log collector which is automatically called on text fail or error even in setup or post methods
 */
public class GlobalLogCollector implements TestExecutionExceptionHandler, LifecycleMethodExecutionExceptionHandler {
    private static final Logger LOGGER = LogManager.getLogger(GlobalLogCollector.class);
    private static LogCollector globalInstance;
    private static final List<ThrowableRunner> COLLECT_CALLBACKS = new LinkedList<>();

    /**
     * Private constructor
     */
    private GlobalLogCollector() {
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
        saveKubeState();
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
        saveKubeState();
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
        saveKubeState();
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
        saveKubeState();
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
        saveKubeState();
        LifecycleMethodExecutionExceptionHandler.super.handleAfterAllMethodExecutionException(context, throwable);
    }

    /**
     * Setup globalLogCollector which is automatically used within {@link CollectLogs} annotation
     *
     * @param globalLogCollector log collector instance
     */
    public static void setupGlobalLogCollector(LogCollector globalLogCollector) {
        globalInstance = globalLogCollector;
    }

    /**
     * Returns globalLogCollector instance
     *
     * @return global log collector instance
     */
    public static LogCollector getGlobalLogCollector() {
        if (globalInstance == null) {
            throw new NullPointerException("Global log collector is not initialized");
        }
        return globalInstance;
    }

    /**
     * Adds callback for running log collecting
     *
     * @param callback callback method with log collecting
     */
    public static void addLogCallback(ThrowableRunner callback) {
        COLLECT_CALLBACKS.add(callback);
    }

    private void saveKubeState() {
        try {
            for (ThrowableRunner runner : COLLECT_CALLBACKS) {
                runner.run();
            }
        } catch (Exception ex) {
            LOGGER.error("Cannot collect all data", ex);
        }
    }
}
