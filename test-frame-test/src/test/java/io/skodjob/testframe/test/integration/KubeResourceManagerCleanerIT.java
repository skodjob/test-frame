/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.test.integration;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.skodjob.testframe.annotations.ResourceManager;
import io.skodjob.testframe.annotations.TestVisualSeparator;
import io.skodjob.testframe.clients.KubeClusterException;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ResourceManager
@TestVisualSeparator
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class KubeResourceManagerCleanerIT {

    @BeforeAll
    void setupAll() {
        KubeResourceManager.getInstance().createResourceWithWait(
                new NamespaceBuilder().withNewMetadata().withName("test").endMetadata().build());
    }

    @BeforeEach
    void setupEach() {
        KubeResourceManager.getInstance().createResourceWithWait(
                new NamespaceBuilder().withNewMetadata().withName("test2").endMetadata().build());
    }

    @AfterAll
    void afterAll() {
        assertNull(KubeResourceManager.getKubeClient().getClient().namespaces().withName("test2").get());
        assertNull(KubeResourceManager.getKubeClient().getClient().namespaces().withName("test3").get());
    }

    @Test
    void createResource() {
        Namespace ns = new NamespaceBuilder().withNewMetadata().withName("test3").endMetadata().build();
        KubeResourceManager.getInstance().createResourceWithWait(ns);
        KubeResourceManager.getInstance().createOrUpdateResourceWithWait(ns);
    }

    @Test
    void testKubeClientNamespacesExists() {
        assertNotNull(KubeResourceManager.getKubeClient().getClient().namespaces().withName("test").get());
        assertNotNull(KubeResourceManager.getKubeClient().getClient().namespaces().withName("test2").get());
        assertNull(KubeResourceManager.getKubeClient().getClient().namespaces().withName("test3").get());
    }

    @Test
    void testKubeCmdClientNamespacesExists() {
        assertNotNull(KubeResourceManager.getKubeCmdClient().get("namespace", "test"));
        assertNotNull(KubeResourceManager.getKubeCmdClient().get("namespace", "test2"));
        assertThrows(KubeClusterException.class, () -> KubeResourceManager.getKubeCmdClient().get("namespace", "test3"));
    }
}
