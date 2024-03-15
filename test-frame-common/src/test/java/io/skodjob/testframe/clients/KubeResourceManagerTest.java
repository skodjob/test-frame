/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.clients;

import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.skodjob.testframe.annotations.ResourceManager;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@EnableKubernetesMockClient(crud = true)
@ResourceManager
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
}
