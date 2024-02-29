/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.test.integration;

import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.skodjob.testframe.annotations.TestVisualSeparator;
import io.skodjob.testframe.clients.KubeClusterException;
import io.skodjob.testframe.resources.ResourceManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@io.skodjob.testframe.annotations.ResourceManager
@TestVisualSeparator
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
    void createResource() {
        ResourceManager.getInstance().createResourceWithWait(
                new NamespaceBuilder().withNewMetadata().withName("test3").endMetadata().build());
    }

    @Test
    void testKubeClientNamespacesExists() {
        assertNotNull(ResourceManager.getKubeClient().getClient().namespaces().withName("test").get());
        assertNotNull(ResourceManager.getKubeClient().getClient().namespaces().withName("test2").get());
        assertNull(ResourceManager.getKubeClient().getClient().namespaces().withName("test3").get());
    }

    @Test
    void testKubeCmdClientNamespacesExists() {
        assertNotNull(ResourceManager.getKubeCmdClient().get("namespace", "test"));
        assertNotNull(ResourceManager.getKubeCmdClient().get("namespace", "test2"));
        assertThrows(KubeClusterException.class, () -> ResourceManager.getKubeCmdClient().get("namespace", "test3"));
    }
}
