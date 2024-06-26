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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
        HashMap<String, String> emptyData = new HashMap<>();
        this.metricsCollector.setCollectedData(emptyData);

        // Execution
        Map<String, Double> results = this.metricsCollector.collectMetricWithLabels("nonexistentMetric");

        // Verification
        assertTrue(results.isEmpty(), "Results should be empty when no relevant data is available");
    }

    @Test
    void testCollectMetricWithLabelsInvalidDataFormat() {
        // Setup
        HashMap<String, String> invalidData = new HashMap<>();
        invalidData.put("data", "metricName{label} xyz");
        this.metricsCollector.setCollectedData(invalidData);

        // Execution
        Map<String, Double> results = this.metricsCollector.collectMetricWithLabels("metricName");

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
    void testCollectSpecificMetricValidData() {
        HashMap<String, String> collectedData = new HashMap<>();
        collectedData.put("data", "value: 123.45");
        this.metricsCollector.setCollectedData(collectedData); // Assuming a setter for testing

        List<Double> results = this.metricsCollector.collectSpecificMetric(Pattern.compile("value: (\\d+\\.\\d+)"));
        assertFalse(results.isEmpty());
        assertEquals(123.45, results.get(0), 0.01);
    }

    @Test
    void testCollectSpecificMetricNoDataMatch() {
        HashMap<String, String> collectedData = new HashMap<>();
        collectedData.put("data", "no match here");
        this.metricsCollector.setCollectedData(collectedData); // Assuming a setter for testing

        List<Double> results = this.metricsCollector.collectSpecificMetric(Pattern.compile("value: (\\d+\\.\\d+)"));
        assertTrue(results.isEmpty());
    }

    @Test
    void testCollectMetricWithLabelsMatchingEntries() {
        HashMap<String, String> collectedData = new HashMap<>();
        collectedData.put("metric", "metricName{label} 100.0");
        this.metricsCollector.setCollectedData(collectedData); // Assuming a setter for testing

        Map<String, Double> results = this.metricsCollector.collectMetricWithLabels("metricName");
        assertFalse(results.isEmpty());
        assertTrue(results.containsKey("metricName{label}"));
        assertEquals(100.0, results.get("metricName{label}"), 0.01);
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
