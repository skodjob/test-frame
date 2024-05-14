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

import java.util.Optional;

import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

/**
 * Enables cleaner extension based on cleanResources value
 */
public class ResourceManagerCleanerExtension implements AfterAllCallback, AfterEachCallback {

    private ResourceManagerCleanerExtension() {
        // Private constructor to prevent instantiation
    }

    /**
     * Enables ResourceManagerCleanerExtension for after All callback
     * @param extensionContext context
     */
    @Override
    public void afterAll(ExtensionContext extensionContext) {
        Optional<ResourceManager> annotation =
                findAnnotation(extensionContext.getRequiredTestClass(), ResourceManager.class);

        if (annotation.isPresent() && annotation.get().cleanResources()) {
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
        Optional<ResourceManager> annotation =
                findAnnotation(extensionContext.getRequiredTestClass(), ResourceManager.class);

        if (annotation.isPresent() && annotation.get().cleanResources()) {
            KubeResourceManager.setTestContext(extensionContext);
            KubeResourceManager.getInstance().deleteResources();
        }
    }
}
