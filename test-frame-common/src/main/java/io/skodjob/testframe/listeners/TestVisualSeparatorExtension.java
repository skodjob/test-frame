/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.listeners;

import io.skodjob.testframe.LoggerUtils;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * jUnit5 specific class which listening on test callbacks
 */
public class TestVisualSeparatorExtension implements BeforeEachCallback, AfterEachCallback {
    Logger LOGGER = LoggerFactory.getLogger(TestVisualSeparatorExtension.class);
    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        LoggerUtils.logSeparator();
        LOGGER.info(String.format("%s.%s-STARTED", extensionContext.getRequiredTestClass().getName(),
                extensionContext.getDisplayName().replace("()", "")));
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
        LOGGER.info(String.format("%s.%s-FINISHED", extensionContext.getRequiredTestClass().getName(),
                extensionContext.getDisplayName().replace("()", "")));
        LoggerUtils.logSeparator();
    }
}
