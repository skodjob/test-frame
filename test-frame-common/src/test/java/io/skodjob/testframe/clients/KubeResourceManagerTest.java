/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.clients;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.skodjob.testframe.annotations.ResourceManager;
import io.skodjob.testframe.annotations.TestVisualSeparator;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@EnableKubernetesMockClient(crud = true)
@ResourceManager
@TestVisualSeparator
public class KubeResourceManagerTest {
    private KubernetesClient kubernetesClient;
    private KubernetesMockServer server;

    @BeforeEach
    void setupClient() {
        KubeResourceManager.getKubeClient().testReconnect(kubernetesClient.getConfiguration());
    }

    @Test
    void testCreateDeleteNamespace() {
        KubeResourceManager.getInstance().createResourceWithWait(new NamespaceBuilder().withNewMetadata().withName("test").endMetadata().build());
        assertNotNull(KubeResourceManager.getKubeClient().getClient().namespaces().withName("test").get());
    }

    @Test
    void testDeleteAllResources() {
        KubeResourceManager.getInstance().createResourceWithWait(new NamespaceBuilder().withNewMetadata().withName("test2").endMetadata().build());
        assertNull(KubeResourceManager.getKubeClient().getClient().namespaces().withName("test").get());
        assertNotNull(KubeResourceManager.getKubeClient().getClient().namespaces().withName("test2").get());
        KubeResourceManager.getInstance().deleteResources();
        assertNull(KubeResourceManager.getKubeClient().getClient().namespaces().withName("test2").get());
    }

    @Test
    void testUpdateResource() {
        Namespace ns = new NamespaceBuilder().withNewMetadata().withName("test3").endMetadata().build();
        KubeResourceManager.getInstance().createResourceWithWait(ns);
        assertNotNull(KubeResourceManager.getKubeClient().getClient().namespaces().withName("test3").get());
        KubeResourceManager.getInstance().updateResource(ns.edit()
                .editMetadata().addToLabels(Collections.singletonMap("test-label", "true")).endMetadata().build());
        assertNotNull(KubeResourceManager.getKubeClient().getClient().namespaces().withName("test3").get()
                .getMetadata().getLabels().get("test-label"));
    }

    @Test
    void testCreateOrUpdateResource() {
        Namespace ns = new NamespaceBuilder().withNewMetadata().withName("test4").endMetadata().build();
        KubeResourceManager.getInstance().createResourceWithWait(ns);
        assertNotNull(KubeResourceManager.getKubeClient().getClient().namespaces().withName("test4").get());
        KubeResourceManager.getInstance().createOrUpdateResourceWithWait(ns);
        assertNotNull(KubeResourceManager.getKubeClient().getClient().namespaces().withName("test4").get());
        KubeResourceManager.getInstance().createOrUpdateResourceWithoutWait(ns);
        assertNotNull(KubeResourceManager.getKubeClient().getClient().namespaces().withName("test4").get());
    }
}
