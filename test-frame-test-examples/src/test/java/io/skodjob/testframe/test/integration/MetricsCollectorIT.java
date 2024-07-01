/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.test.integration;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.skodjob.testframe.MetricsCollector;
import io.skodjob.testframe.MetricsComponent;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class MetricsCollectorIT extends AbstractIT {

    @Test
    void testCollectMetrics() throws IOException {
        //Create deployment
        List<HasMetadata> resources = KubeResourceManager.getKubeClient()
            .readResourcesFromFile(getClass().getClassLoader().getResourceAsStream("metrics-example.yaml"));

        KubeResourceManager.getInstance().createResourceWithWait(resources.toArray(new HasMetadata[0]));

        // Check deployment is not null
        assertNotNull(KubeResourceManager.getKubeClient().getClient().namespaces().withName("metrics-test").get());
        assertNotNull(KubeResourceManager.getKubeClient().getClient().apps().deployments()
            .inNamespace("metrics-test").withName("prometheus-example").get());
        assertNotNull(KubeResourceManager.getKubeClient().getClient().apps().deployments()
            .inNamespace("metrics-test").withName("scraper-pod").get());

        // Create metrics collector
        MetricsCollector collector = new MetricsCollector.Builder()
            .withNamespaceName("metrics-test")
            .withScraperPodName(KubeResourceManager.getKubeClient()
                .listPodsByPrefixInName("metrics-test", "scraper-pod").get(0)
                .getMetadata().getName())
            .withComponent(new MetricsComponent() {
                public int getDefaultMetricsPort() {
                    return 8080;
                }

                public String getDefaultMetricsPath() {
                    return "/metrics";
                }

                public LabelSelector getLabelSelector() {
                    return new LabelSelectorBuilder()
                        .withMatchLabels(Map.of("app", "prometheus-example-app"))
                        .build();
                }
            })
            .build();

        assertDoesNotThrow(() -> collector.collectMetricsFromPods(30000)); // timeout in milliseconds
        Map<String, String> metrics = collector.getCollectedData();
        assertTrue(metrics.containsKey(KubeResourceManager.getKubeClient()
            .listPodsByPrefixInName("metrics-test", "prometheus-example").get(0)
            .getMetadata().getName()));
    }

    @Test
    void testCollectMetricsWitAutoDeployedPod() throws IOException {
        //Create deployment
        List<HasMetadata> resources = KubeResourceManager.getKubeClient()
            .readResourcesFromFile(getClass().getClassLoader().getResourceAsStream("metrics-example.yaml"))
            .stream().filter(resource -> !resource.getMetadata().getName().equals("scraper-pod")).toList();

        KubeResourceManager.getInstance().createResourceWithWait(resources.toArray(new HasMetadata[0]));

        // Check deployment is not null
        assertNotNull(KubeResourceManager.getKubeClient().getClient().namespaces().withName("metrics-test").get());
        assertNotNull(KubeResourceManager.getKubeClient().getClient().apps().deployments()
            .inNamespace("metrics-test").withName("prometheus-example").get());

        // Create metrics collector
        MetricsCollector collector = new MetricsCollector.Builder()
            .withNamespaceName("metrics-test")
            .withDeployScraperPod()
            .withScraperPodName("test-scraper-pod")
            .withComponent(new MetricsComponent() {
                public int getDefaultMetricsPort() {
                    return 8080;
                }

                public String getDefaultMetricsPath() {
                    return "/metrics";
                }

                public LabelSelector getLabelSelector() {
                    return new LabelSelectorBuilder()
                        .withMatchLabels(Map.of("app", "prometheus-example-app"))
                        .build();
                }
            })
            .build();

        assertDoesNotThrow(() -> collector.collectMetricsFromPods(30000)); // timeout in milliseconds
        Map<String, String> metrics = collector.getCollectedData();
        assertTrue(metrics.containsKey(KubeResourceManager.getKubeClient()
            .listPodsByPrefixInName("metrics-test", "prometheus-example").get(0)
            .getMetadata().getName()));
    }
}
