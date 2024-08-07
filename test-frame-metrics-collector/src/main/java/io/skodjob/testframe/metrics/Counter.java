/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.metrics;

import java.util.Map;

/**
 * Counter metric type
 */
public class Counter extends Metric {
    double value;

    /**
     * Constructor
     *
     * @param name   name of metric
     * @param labels labels
     * @param value  value
     */
    public Counter(String name, Map<String, String> labels, double value) {
        super(name, labels, MetricType.COUNTER);
        this.value = value;
    }

    /**
     * Metric string representation
     *
     * @return string representation
     */
    @Override
    public String toString() {
        return "Counter{" +
            "name='" + name + '\'' +
            ", labels=" + labels +
            ", value=" + value +
            ", type=" + type +
            '}';
    }
}
