/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.listeners;

import io.skodjob.testframe.annotations.ResourceManager;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Enables cleaner extension based on cleanResources value
 */
public class ResourceManagerCleanerExtension implements AfterAllCallback, AfterEachCallback {

    /**
     * Enables ResourceManagerCleanerExtension for afterAll callback
     * @param extensionContext context
     */
    @Override
    public void afterAll(ExtensionContext extensionContext) {
        Class<?> testClass = extensionContext.getRequiredTestClass();
        ResourceManager annotation = testClass.getAnnotation(ResourceManager.class);
        if (annotation != null && annotation.cleanResources()) {
            KubeResourceManager.setTestContext(extensionContext);
            KubeResourceManager.getInstance().deleteResources();
        }
    }

    /**
     * Enables ResourceManagerCleanerExtension for afterEach callback
     * @param extensionContext context
     */
    @Override
    public void afterEach(ExtensionContext extensionContext) {
        Class<?> testClass = extensionContext.getRequiredTestClass();
        ResourceManager annotation = testClass.getAnnotation(ResourceManager.class);
        if (annotation != null && annotation.cleanResources()) {
            KubeResourceManager.setTestContext(extensionContext);
            KubeResourceManager.getInstance().deleteResources();
        }
    }
}
