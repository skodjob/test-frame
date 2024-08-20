/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.listeners;

import io.skodjob.testframe.utils.LoggerUtils;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * jUnit5 specific class which listening on test callbacks
 */
public class TestVisualSeparatorExtension implements BeforeEachCallback, AfterEachCallback,
    BeforeAllCallback, AfterAllCallback {
    private final Logger logger = LoggerFactory.getLogger(TestVisualSeparatorExtension.class);

    private TestVisualSeparatorExtension() {
        // Private constructor to prevent instantiation
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) {
        LoggerUtils.logSeparator();
        logger.info("TestClass {} STARTED", extensionContext.getRequiredTestClass().getName());
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) {
        logger.info("TestClass {} FINISHED", extensionContext.getRequiredTestClass().getName());
        LoggerUtils.logSeparator();
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) {
        LoggerUtils.logSeparator();
        logger.info("Test {}.{} STARTED", extensionContext.getRequiredTestClass().getName(),
            extensionContext.getDisplayName().replace("()", ""));
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) {
        logger.info("Test {}.{} FINISHED", extensionContext.getRequiredTestClass().getName(),
            extensionContext.getDisplayName().replace("()", ""));
        LoggerUtils.logSeparator();
    }
}
