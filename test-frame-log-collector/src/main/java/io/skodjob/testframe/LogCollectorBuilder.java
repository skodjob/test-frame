/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe;

import java.util.Arrays;
import java.util.List;

/**
 * Builder class for the {@link LogCollector}
 */
public class LogCollectorBuilder {

    private String rootFolderPath;
    private List<String> resources;

    /**
     * Constructor for creating {@link LogCollectorBuilder} with parameters from
     * current instance of {@link LogCollector}.
     *
     * @param logCollector  current instance of {@link LogCollector}
     */
    public LogCollectorBuilder(LogCollector logCollector) {
        this.rootFolderPath = logCollector.rootFolderPath;
        this.resources = logCollector.resources;
    }

    /**
     * Empty constructor, we can use the "with" methods to build the LogCollector's configuration
     */
    public LogCollectorBuilder() {
        // empty constructor
    }

    /**
     * Method for setting the rootFolderPath, where the logs collection will happen
     *
     * @param rootFolderPath    folder path for the root folder, where the logs should be collected into
     *
     * @return  {@link LogCollectorBuilder} object
     */
    public LogCollectorBuilder withRootFolderPath(String rootFolderPath) {
        this.rootFolderPath = rootFolderPath;
        return this;
    }

    /**
     * Method for setting the resources, which YAML descriptions should be collected as part of the log collection
     *
     * @param resources    array of resources
     *
     * @return  {@link LogCollectorBuilder} object
     */
    public LogCollectorBuilder withResources(String... resources) {
        this.resources = Arrays.stream(resources).toList();
        return this;
    }

    /**
     * Getter returning currently configured {@link #rootFolderPath}
     *
     * @return {@link #rootFolderPath}
     */
    public String getRootFolderPath() {
        return this.rootFolderPath;
    }

    /**
     * Getter returning currently configured {@link #resources} in {@link List} object
     *
     * @return {@link #resources}
     */
    public List<String> getResources() {
        return this.resources;
    }

    /**
     * Method for building the {@link LogCollector} object
     *
     * @return {@link LogCollector} configured by the specified parameters
     */
    public LogCollector build() {
        return new LogCollector(this);
    }
}
