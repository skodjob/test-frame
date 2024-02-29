/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.test.integration;

import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.skodjob.testframe.resources.ResourceManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@io.skodjob.testframe.annotations.ResourceManager
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ResourceManagerIT {

    @BeforeAll
    void setupAll() {
        ResourceManager.getInstance().createResourceWithWait(
                new NamespaceBuilder().withNewMetadata().withName("test").endMetadata().build());
    }

    @BeforeEach
    void setupEach() {
        ResourceManager.getInstance().createResourceWithWait(
                new NamespaceBuilder().withNewMetadata().withName("test2").endMetadata().build());
    }

    @Test
    void test() {
        assertNotNull(ResourceManager.getKubeClient().getClient().namespaces().withName("test").get());
        assertNotNull(ResourceManager.getKubeClient().getClient().namespaces().withName("test2").get());
    }
}
