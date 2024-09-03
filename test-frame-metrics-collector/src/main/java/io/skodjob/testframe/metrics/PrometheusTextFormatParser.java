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
        String type = "";
        Histogram currentHistogram = null;
        Summary currentSummary = null;

        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(" ");

            if (line.startsWith("#")) {
                if (line.contains("TYPE")) {
                    type = parts[3];
                }
                continue; // Skip comments and help definition
            }

            if (parts.length == 2) {
                String metricNameAndLabels = parts[0];
                double value = Double.parseDouble(parts[1]);

                String[] nameAndLabels = parseNameAndLabels(metricNameAndLabels);
                String name = nameAndLabels[0];
                Map<String, String> labels = parseLabels(nameAndLabels[1]);
                // `customLabels` represents labels that are not part of the Prometheus metrics
                // (like buckets or summary) - the `le` and `quantile` labels are removed,
                // because they are parsed in a different way
                Map<String, String> customLabels = getCustomLabels(labels);

                if (name.endsWith("_total")) {
                    metrics.add(new Counter(name, customLabels, line, value));
                } else if (name.contains("_bucket")) {
                    if (currentHistogram == null || !currentHistogram.name.equals(name)
                        || !currentHistogram.labels.equals(customLabels)) {
                        currentHistogram = new Histogram(name, customLabels, line);
                        metrics.add(currentHistogram);
                    }
                    double upperBound = labels.get("le").contains("+Inf") ?
                        Double.MAX_VALUE : Double.parseDouble(labels.get("le"));
                    currentHistogram.addBucket(upperBound, value);
                } else if (name.endsWith("_sum")) {
                    if (currentHistogram != null && currentHistogram.name.equals(name.replace("_sum", "_bucket"))
                        && currentHistogram.labels.equals(customLabels)) {
                        currentHistogram.setSum(value);
                    } else if (currentSummary != null && currentSummary.name.equals(name.replace("_sum", ""))
                        && currentSummary.labels.equals(customLabels)) {
                        currentSummary.setSum(value);
                    }
                } else if (name.endsWith("_count")) {
                    if (currentHistogram != null && currentHistogram.name.equals(name.replace("_count", "_bucket"))
                        && currentHistogram.labels.equals(customLabels)) {
                        currentHistogram.setCount((int) value);
                    } else if (currentSummary != null && currentSummary.name.equals(name.replace("_count", ""))
                        && currentSummary.labels.equals(customLabels)) {
                        currentSummary.setCount((int) value);
                    } else if (type != null) {
                        if (type.equals("gauge")) {
                            metrics.add(new Gauge(name, customLabels, line, value));
                        } else if (type.equals("counter")) {
                            metrics.add(new Counter(name, customLabels, line, value));
                        }
                    }
                } else if (name.contains("{quantile=")) {
                    if (currentSummary == null || !currentSummary.name.equals(name)
                        || !currentSummary.labels.equals(customLabels)) {
                        currentSummary = new Summary(name, customLabels, line);
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

    /**
     * Method that "removes" Prometheus labels (connected to buckets or summary) and returns just the custom labels
     * provided by the user or application.
     *
     * @param labels    parsed labels from the Prometheus metric
     * @return  custom labels from the metric
     */
    private static Map<String, String> getCustomLabels(Map<String, String> labels) {
        Map<String, String> customLabels = new HashMap<>(labels);

        customLabels.remove("le");
        customLabels.remove("quantile");

        return customLabels;
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
