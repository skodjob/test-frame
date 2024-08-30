/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.metrics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser class
 */
public class PrometheusTextFormatParser {

    private PrometheusTextFormatParser() {
        // empty constructor
    }

    /**
     * Parse metrics from string data
     *
     * @param data string data in prometheus text format
     * @return parsed data in list of Metric
     * @throws IOException IOException
     */
    public static List<Metric> parse(String data) throws IOException {
        List<Metric> metrics = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new StringReader(data));
        String line;
        Histogram currentHistogram = null;
        Summary currentSummary = null;

        while ((line = reader.readLine()) != null) {
            if (line.startsWith("#")) {
                continue; // Skip comments and type/help definitions
            }
            String[] parts = line.split(" ");
            if (parts.length == 2) {
                String metricNameAndLabels = parts[0];
                double value = Double.parseDouble(parts[1]);

                String[] nameAndLabels = parseNameAndLabels(metricNameAndLabels);
                String name = nameAndLabels[0];
                Map<String, String> labels = parseLabels(nameAndLabels[1]);

                if (name.endsWith("_total")) {
                    metrics.add(new Counter(name, labels, line, value));
                } else if (name.contains("_bucket")) {
                    if (currentHistogram == null || !currentHistogram.name.equals(name)) {
                        currentHistogram = new Histogram(name, labels, line);
                        metrics.add(currentHistogram);
                    }
                    double upperBound = labels.get("le").contains("+Inf") ?
                        Double.MAX_VALUE : Double.parseDouble(labels.get("le"));
                    currentHistogram.addBucket(upperBound, value);
                } else if (name.endsWith("_sum")) {
                    if (currentHistogram != null && currentHistogram.name.equals(name.replace("_sum", "_bucket"))) {
                        currentHistogram.setSum(value);
                    } else if (currentSummary != null && currentSummary.name.equals(name.replace("_sum", ""))) {
                        currentSummary.setSum(value);
                    }
                } else if (name.endsWith("_count")) {
                    if (currentHistogram != null && currentHistogram.name.equals(name.replace("_count", "_bucket"))) {
                        currentHistogram.setCount((int) value);
                    } else if (currentSummary != null && currentSummary.name.equals(name.replace("_count", ""))) {
                        currentSummary.setCount((int) value);
                    }
                } else if (name.contains("{quantile=")) {
                    if (currentSummary == null || !currentSummary.name.equals(name)) {
                        currentSummary = new Summary(name, labels, line);
                        metrics.add(currentSummary);
                    }
                    double quantile = Double.parseDouble(labels.get("quantile"));
                    currentSummary.addQuantile(quantile, value);
                } else {
                    metrics.add(new Gauge(name, labels, line, value));
                }
            }
        }
        return metrics;
    }

    private static String[] parseNameAndLabels(String metricNameAndLabels) {
        int labelStartIndex = metricNameAndLabels.indexOf('{');
        if (labelStartIndex == -1) {
            return new String[]{metricNameAndLabels, ""};
        }
        String name = metricNameAndLabels.substring(0, labelStartIndex);
        String labels = metricNameAndLabels.substring(labelStartIndex);
        return new String[]{name, labels};
    }

    private static Map<String, String> parseLabels(String labelString) {
        Map<String, String> labels = new HashMap<>();
        if (labelString.isEmpty()) {
            return labels;
        }
        labelString = labelString.substring(1, labelString.length() - 1); // Remove curly braces
        String[] labelPairs = labelString.split(",");
        for (String labelPair : labelPairs) {
            String[] keyValue = labelPair.split("=");
            String key = keyValue[0];
            String value = keyValue[1].replaceAll("^\"|\"$", ""); // Remove quotes
            labels.put(key, value);
        }
        return labels;
    }
}
