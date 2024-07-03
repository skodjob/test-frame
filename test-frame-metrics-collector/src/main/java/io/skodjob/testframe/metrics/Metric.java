/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.metrics;

import java.util.Map;

/**
 * Abstract representation of metric
 */
public abstract class Metric {
    String name;
    Map<String, String> labels;
    MetricType type;

    /**
     * constructor
     *
     * @param name   name of metric
     * @param labels labels
     * @param type   type
     */
    Metric(String name, Map<String, String> labels, MetricType type) {
        this.name = name;
        this.labels = labels;
        this.type = type;
    }

    /**
     * Return labels
     *
     * @return labels
     */
    public Map<String, String> getLabels() {
        return labels;
    }

    /**
     * Return type
     *
     * @return type of metric
     */
    public MetricType getType() {
        return type;
    }

    /**
     * Get name
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Metric string representation
     *
     * @return string representation
     */
    @Override
    public String toString() {
        return "Metric{" +
            "name='" + name + '\'' +
            ", labels=" + labels +
            ", type=" + type +
            '}';
    }
}
