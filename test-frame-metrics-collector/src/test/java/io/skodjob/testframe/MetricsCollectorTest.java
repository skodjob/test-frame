/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.skodjob.testframe.exceptions.MetricsCollectionException;
import io.skodjob.testframe.metrics.Gauge;
import io.skodjob.testframe.metrics.Metric;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.security.InvalidParameterException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class MetricsCollectorTest {

    private MetricsCollector metricsCollector;

    @BeforeEach
    void setUp() {
        this.metricsCollector = new MetricsCollector.Builder()
            .withNamespaceName("namespace")
            .withScraperPodName("scraperPod")
            .withComponent(new DummyMetricsComponent())
            .build();
    }

    @Test
    void testCollectMetricWithLabelsNoDataAvailable() {
        // Setup
        HashMap<String, List<Metric>> emptyData = new HashMap<>();
        this.metricsCollector.setCollectedData(emptyData);

        // Execution
        List<Metric> results = this.metricsCollector.collectMetricWithLabels("pod", "nonexistentMetric");

        // Verification
        assertTrue(results.isEmpty(), "Results should be empty when no relevant data is available");
    }

    @Test
    void testCollectMetricWithLabelsInvalidDataFormat() {
        // Setup
        HashMap<String, List<Metric>> invalidData = new HashMap<>();
        invalidData.put("pod", Collections.singletonList(new Gauge("metric_name",
            Collections.singletonMap("label", "value"), "metric_name {label=value} 32", 32)));
        this.metricsCollector.setCollectedData(invalidData);

        // Execution
        List<Metric> results = this.metricsCollector.collectMetricWithLabels("pod", "metricName");

        // Verification
        assertTrue(results.isEmpty(), "Results should be empty with invalid data format");
    }

    @Test
    void testBuilderInvalidParameters() {
        // Verification
        assertThrows(InvalidParameterException.class, () -> new MetricsCollector.Builder().build(),
            "Should throw InvalidParameterException when required parameters are not set");
    }

    @Test
    void testCollectMetricWithLabelsMatchingEntries() {
        HashMap<String, List<Metric>> invalidData = new HashMap<>();
        invalidData.put("pod", Collections.singletonList(new Gauge("metric_name",
            Collections.singletonMap("label", "value"), "metric_name {label=value} 100", 100)));
        this.metricsCollector.setCollectedData(invalidData);

        List<Metric>  results = this.metricsCollector.collectMetricWithLabels("pod", "label");
        assertFalse(results.isEmpty());
        assertEquals(100.0, ((Gauge)results.get(0)).getValue(), 0.01);
    }

    @Test
    public void testCollectMetricsHandlesNPE() {
        MetricsCollector collector = Mockito.spy(metricsCollector);
        Mockito.doThrow(new NullPointerException("Null pointer access"))
            .when(collector).collectMetricsFromPodsWithoutWait();

        // Assert that a MetricsCollectionException is thrown when an NPE occurs
        MetricsCollectionException ex = assertThrows(MetricsCollectionException.class, () ->
            collector.collectMetricsFromPods(1000));

        // Verify that the exception message contains specific information about the NPE
        assertTrue(ex.getMessage().contains("Null pointer access"),
            "Exception message should indicate the nature of the NPE.");
    }

    static class DummyMetricsComponent implements MetricsComponent {
        @Override
        public int getDefaultMetricsPort() {
            return 8080; // Return a dummy port typically used for HTTP services
        }

        @Override
        public String getDefaultMetricsPath() {
            return "/metrics"; // Return a dummy path where metrics might be collected
        }

        @Override
        public LabelSelector getLabelSelector() {
            final Map<String, String> matchLabels = new HashMap<>();

            matchLabels.put("app", "mockApp"); // Return a dummy selector

            return new LabelSelectorBuilder()
                .withMatchLabels(matchLabels)
                .build();
        }
    }
}
