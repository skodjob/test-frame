package io.skodjob.testframe;

import io.skodjob.testframe.metrics.Counter;
import io.skodjob.testframe.metrics.Gauge;
import io.skodjob.testframe.metrics.Histogram;
import io.skodjob.testframe.metrics.Metric;
import io.skodjob.testframe.metrics.PrometheusTextFormatParser;
import io.skodjob.testframe.metrics.Summary;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PrometheusTextFormatParserTest {

    @Test
    void testParseGaugeMetric() throws IOException {
        String data = "# TYPE my_gauge gauge\n" +
            "my_gauge{label=\"value\"} 123.45\n";
        List<Metric> metrics = PrometheusTextFormatParser.parse(data);
        assertEquals(1, metrics.size());
        Metric m = metrics.get(0);
        assertTrue(m instanceof Gauge);
        assertEquals(123.45, ((Gauge) m).getValue(), 0.0001);
        assertEquals("value", m.getLabels().get("label"));
    }

    @Test
    void testParseCounterMetric() throws IOException {
        String data = "# TYPE my_counter counter\n" +
            "my_counter_total{label=\"cnt\"} 10\n" +
            "my_counter_count{label=\"cnt\"} 10\n";
        List<Metric> metrics = PrometheusTextFormatParser.parse(data);
        assertEquals(2, metrics.size());
        for (Metric m : metrics) {
            assertTrue(m instanceof Counter);
            assertEquals(10, ((Counter) m).getValue(), 0.0001);
        }
    }

    @Test
    void testParseHistogramMetrics() throws IOException {
        String data = "# TYPE my_histogram histogram\n" +
            "my_histogram_bucket{le=\"0.1\"} 2\n" +
            "my_histogram_bucket{le=\"1.0\"} 5\n" +
            "my_histogram_bucket{le=\"+Inf\"} 7\n" +
            "my_histogram_sum 10.0\n" +
            "my_histogram_count 7\n";
        List<Metric> metrics = PrometheusTextFormatParser.parse(data);
        assertEquals(1, metrics.size());
        Histogram hist = (Histogram) metrics.get(0);
        assertEquals(10.0, hist.getSum(), 0.0001);
        assertEquals(7, hist.getCount());
        Map<Double, Double> buckets = hist.getBuckets();
        assertEquals(3, buckets.size());
        assertEquals(2, buckets.get(0.1), 0.0001);
        assertEquals(5, buckets.get(1.0), 0.0001);
        assertEquals(7, buckets.get(Double.MAX_VALUE), 0.0001);
    }

    @Test
    void testParseSummaryMetrics() throws IOException {
        String data = "# TYPE my_summary summary\n" +
            "my_summary{quantile=\"0.5\"} 3.0\n" +
            "my_summary{quantile=\"0.9\"} 4.0\n" +
            "my_summary_sum 7.0\n" +
            "my_summary_count 2\n";
        List<Metric> metrics = PrometheusTextFormatParser.parse(data);
        // Only one summary should be created.
        assertEquals(1, metrics.size());
        Summary summary = (Summary) metrics.get(0);
        assertEquals(7.0, summary.getSum(), 0.0001);
        assertEquals(2, summary.getCount());
        Map<Double, Double> quantiles = summary.getQuantiles();
        assertEquals(2, quantiles.size());
        assertEquals(3.0, quantiles.get(0.5), 0.0001);
        assertEquals(4.0, quantiles.get(0.9), 0.0001);
    }

    @Test
    void testInvalidLineSkipped() throws IOException {
        String data = "invalid_line_without_space";
        List<Metric> metrics = PrometheusTextFormatParser.parse(data);
        assertEquals(0, metrics.size());
    }

    @Test
    void testParseAllMetrics() throws IOException {
        String data = "# TYPE jvm_memory_used_bytes gauge\n" +
            "jvm_memory_used_bytes{area=\"nonheap\",id=\"CodeHeap 'profiled nmethods'\",} 1.635584E7\n" +
            "jvm_memory_used_bytes{area=\"heap\",id=\"G1 Survivor Space\",} 1981904.0\n" +
            "jvm_memory_used_bytes{area=\"heap\",id=\"G1 Old Gen\",} 1.76768E7\n" +
            "jvm_memory_used_bytes{area=\"nonheap\",id=\"Metaspace\",} 4.9460992E7\n" +
            "jvm_memory_used_bytes{area=\"nonheap\",id=\"CodeHeap 'non-nmethods'\",} 1374848.0\n" +
            "jvm_memory_used_bytes{area=\"heap\",id=\"G1 Eden Space\",} 2097152.0\n" +
            "jvm_memory_used_bytes{area=\"nonheap\",id=\"Compressed Class Space\",} 5468744.0\n" +
            "jvm_memory_used_bytes{area=\"nonheap\",id=\"CodeHeap 'non-profiled nmethods'\",} 4937600.0\n";
        List<Metric> metrics = PrometheusTextFormatParser.parse(data);

        assertEquals(8, metrics.size());
    }
}
