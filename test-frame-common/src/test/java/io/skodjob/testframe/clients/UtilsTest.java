/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.clients;

import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodConditionBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.skodjob.testframe.annotations.ResourceManager;
import io.skodjob.testframe.annotations.TestVisualSeparator;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.utils.KubeUtils;
import io.skodjob.testframe.utils.PodUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@EnableKubernetesMockClient(crud = true)
@ResourceManager
@TestVisualSeparator
class UtilsTest {
    private KubernetesClient kubernetesClient;
    private KubernetesMockServer server;

    @BeforeEach
    void setupClient() {
    }

    @Test
    void testPodUtils() {
        KubeResourceManager.get().kubeClient().testReconnect(kubernetesClient);

        KubeResourceManager.get().createResourceWithWait(
            new NamespaceBuilder().withNewMetadata().withName("test").endMetadata().build());
        KubeResourceManager.get().createResourceWithoutWait(
            new PodBuilder()
                .withNewMetadata()
                    .withName("test-pod")
                    .withNamespace("test")
                    .addToLabels("test-label", "true")
                .endMetadata()
                .withNewStatus()
                    .withPhase("Running")
                    .withConditions(new PodConditionBuilder()
                        .withType("Ready")
                        .withStatus("True")
                        .withMessage("Ready")
                        .build())
                .endStatus()
                .build());

        LabelSelector lb = new LabelSelectorBuilder()
            .withMatchLabels(Collections.singletonMap("test-label", "true")).build();

        assertNotNull(KubeResourceManager.get().kubeClient().getClient().namespaces().withName("test").get());

        PodUtils.waitForPodsReady("test", false, () -> {
        });
        PodUtils.verifyThatPodsAreStable("test", lb);
        assertNotNull(PodUtils.podSnapshot("test", lb).get("test-pod"));
    }

    @Test
    void testKubeUtils() {
        KubeResourceManager.get().kubeClient().testReconnect(kubernetesClient);

        KubeResourceManager.get().createResourceWithWait(
            new NamespaceBuilder().withNewMetadata().withName("test").endMetadata().build());

        KubeUtils.labelNamespace("test", "test-label", "true");
        assertEquals("true", KubeResourceManager.get().kubeClient().getClient()
            .namespaces().withName("test").get().getMetadata().getLabels().get("test-label"));
    }
}
