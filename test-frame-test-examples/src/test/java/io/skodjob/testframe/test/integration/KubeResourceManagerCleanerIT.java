/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.test.integration;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.skodjob.testframe.clients.KubeClusterException;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class KubeResourceManagerCleanerIT extends AbstractIT {

    @BeforeAll
    void setupAll() {
        KubeResourceManager.getInstance().createResourceWithWait(
            new NamespaceBuilder().withNewMetadata().withName(nsName1).endMetadata().build());
    }

    @BeforeEach
    void setupEach() {
        KubeResourceManager.getInstance().createResourceWithWait(
            new NamespaceBuilder().withNewMetadata().withName(nsName2).endMetadata().build());
    }

    @AfterAll
    void afterAll() {
        assertNull(KubeResourceManager.getKubeClient().getClient().namespaces().withName(nsName2).get());
        assertNull(KubeResourceManager.getKubeClient().getClient().namespaces().withName(nsName3).get());
        assertTrue(isDeleteHandlerCalled.get());
    }

    @Test
    void createResource() {
        Namespace ns = new NamespaceBuilder().withNewMetadata().withName(nsName3).endMetadata().build();
        KubeResourceManager.getInstance().createResourceWithWait(ns);
        assertTrue(isCreateHandlerCalled.get());
        KubeResourceManager.getInstance().createOrUpdateResourceWithWait(ns);
        assertNotNull(KubeResourceManager.getKubeClient().getClient().namespaces().withName(nsName3).get()
            .getMetadata().getLabels().get("test-label"));
        KubeResourceManager.getInstance().printAllResources(Level.INFO);
    }

    @Test
    void testKubeClientNamespacesExists() {
        assertNotNull(KubeResourceManager.getKubeClient().getClient().namespaces().withName(nsName1).get());
        assertNotNull(KubeResourceManager.getKubeClient().getClient().namespaces().withName(nsName2).get());
        assertNull(KubeResourceManager.getKubeClient().getClient().namespaces().withName(nsName3).get());
    }

    @Test
    void testKubeCmdClientNamespacesExists() {
        assertNotNull(KubeResourceManager.getKubeCmdClient().get("namespace", nsName1));
        assertNotNull(KubeResourceManager.getKubeCmdClient().get("namespace", nsName2));
        assertThrows(KubeClusterException.class, () ->
            KubeResourceManager.getKubeCmdClient().get("namespace", nsName3));
    }
}
