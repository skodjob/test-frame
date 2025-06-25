/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.utils;

import io.fabric8.kubernetes.api.builder.Visitor;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodConditionBuilder;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.PodListBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.annotations.ResourceManager;
import io.skodjob.testframe.annotations.TestVisualSeparator;
import io.skodjob.testframe.clients.KubeClient;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ResourceManager
@TestVisualSeparator
class UtilsTest {
    static KubeResourceManager kubeResourceManager = mock(KubeResourceManager.class);
    static KubernetesClient kubernetesClient = mock(KubernetesClient.class);
    static KubeClient kubeClient = mock(KubeClient.class);

    @BeforeAll
    static void setup() {
        when(kubeResourceManager.kubeClient()).thenReturn(kubeClient);
        when(kubeClient.getClient()).thenReturn(kubernetesClient);
    }

    @Test
    void testPodUtils() {
        try (MockedStatic<KubeResourceManager> mockedStatic = mockStatic(KubeResourceManager.class)) {
            when(KubeResourceManager.get()).thenReturn(kubeResourceManager);

            @SuppressWarnings("unchecked")
            MixedOperation<Pod, PodList, PodResource> podsOperation = mock(MixedOperation.class);
            @SuppressWarnings("unchecked")
            NonNamespaceOperation<Pod, PodList, PodResource> podsInNamespace = mock(NonNamespaceOperation.class);
            @SuppressWarnings("unchecked")
            FilterWatchListDeletable<Pod, PodList, PodResource> filteredPods = mock(FilterWatchListDeletable.class);

            List<Pod> pods = List.of(
                new PodBuilder()
                    .withNewMetadata()
                        .withName("test-pod")
                            .withNamespace("test")
                            .addToLabels("test-label", "true")
                        .withUid("uid")
                    .endMetadata()
                    .withNewStatus()
                    .withPhase("Running")
                    .withConditions(new PodConditionBuilder()
                        .withType("Ready")
                        .withStatus("True")
                        .withMessage("Ready")
                        .build())
                    .endStatus()
                    .build()
            );

            when(kubernetesClient.pods()).thenReturn(podsOperation);
            when(podsOperation.inNamespace(any())).thenReturn(podsInNamespace);
            when(podsInNamespace.withLabelSelector(any(LabelSelector.class))).thenReturn(filteredPods);
            when(podsInNamespace.list()).thenReturn(new PodListBuilder().withItems(pods.toArray(new Pod[0])).build());
            when(filteredPods.list()).thenReturn(new PodListBuilder().withItems(pods.toArray(new Pod[0])).build());

            LabelSelector lb = new LabelSelectorBuilder()
                .withMatchLabels(Collections.singletonMap("test-label", "true")).build();

            PodUtils.waitForPodsReady("test", false, () -> {});
            PodUtils.verifyThatPodsAreStable("test", lb);
            assertNotNull(PodUtils.podSnapshot("test", lb).get("test-pod"));
        }
    }

    @Test
    void testKubeUtils() {
        try (MockedStatic<KubeResourceManager> mockedStatic = mockStatic(KubeResourceManager.class)) {
            when(KubeResourceManager.get()).thenReturn(kubeResourceManager);

            @SuppressWarnings("unchecked")
            NonNamespaceOperation<Namespace, NamespaceList,
                Resource<Namespace>> namespaceOperation = mock(NonNamespaceOperation.class);

            @SuppressWarnings("unchecked")
            Resource<Namespace> namespaceResource = mock(Resource.class);

            Namespace labeledNamespace = new NamespaceBuilder()
                .withNewMetadata()
                    .withName("test")
                    .addToLabels("test-label", "true")
                .endMetadata()
                .build();

            when(kubeClient.namespaceExists(any())).thenReturn(true);
            when(kubernetesClient.namespaces()).thenReturn(namespaceOperation);
            when(namespaceOperation.withName(any())).thenReturn(namespaceResource);
            when(namespaceResource.edit(any(Visitor.class))).thenReturn(labeledNamespace);
            when(namespaceResource.get()).thenReturn(labeledNamespace);

            KubeUtils.labelNamespace("test", "test-label", "true");
            assertEquals("true", KubeResourceManager.get().kubeClient().getClient()
                .namespaces().withName("test").get().getMetadata().getLabels().get("test-label"));
        }
    }
}
