/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.listeners;

import io.skodjob.testframe.resources.ResourceManager;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * jUnit5 specific class which listening on test callbacks
 */
public class ResourceManagerContextHandler implements BeforeAllCallback, BeforeEachCallback, AfterAllCallback, AfterEachCallback {

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        ResourceManager.setTestContext(extensionContext);
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        ResourceManager.setTestContext(extensionContext);
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        ResourceManager.setTestContext(extensionContext);
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
        ResourceManager.setTestContext(extensionContext);
    }
}
