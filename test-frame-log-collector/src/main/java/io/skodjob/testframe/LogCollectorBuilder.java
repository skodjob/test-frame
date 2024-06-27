/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe;

import io.skodjob.testframe.clients.KubeClient;
import io.skodjob.testframe.clients.cmdClient.KubeCmdClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builder class for the {@link LogCollector}
 */
public class LogCollectorBuilder {

    private String rootFolderPath;
    private List<String> namespacedResources;
    private List<String> clusterWideResources;
    private KubeClient kubeClient;
    private KubeCmdClient<?> kubeCmdClient;

    /**
     * Constructor for creating {@link LogCollectorBuilder} with parameters from
     * current instance of {@link LogCollector}.
     *
     * @param logCollector current instance of {@link LogCollector}
     */
    public LogCollectorBuilder(LogCollector logCollector) {
        this.rootFolderPath = logCollector.rootFolderPath;
        this.namespacedResources = logCollector.namespacedResources;
        this.clusterWideResources = logCollector.clusterWideResources;
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
     * @param rootFolderPath folder path for the root folder, where the logs should be collected into
     * @return {@link LogCollectorBuilder} object
     */
    public LogCollectorBuilder withRootFolderPath(String rootFolderPath) {
        this.rootFolderPath = rootFolderPath;
        return this;
    }

    /**
     * Method for setting the  namespaced resources, which YAML
     * descriptions should be collected as part of the log collection
     *
     * @param resources array of resources
     * @return {@link LogCollectorBuilder} object
     */
    public LogCollectorBuilder withNamespacedResources(String... resources) {
        this.namespacedResources = Arrays.stream(resources).toList()
            .stream()
            .filter(resource -> !resource.equals(CollectorConstants.POD) && !resource.equals(CollectorConstants.PODS))
            .collect(Collectors.toList());

        return this;
    }

    /**
     * Method for setting the  cluster wide resources, which YAML
     * descriptions should be collected as part of the log collection
     *
     * @param resources array of resources
     * @return {@link LogCollectorBuilder} object
     */
    public LogCollectorBuilder withClusterWideResources(String... resources) {
        this.clusterWideResources = new ArrayList<>(Arrays.stream(resources).toList());

        return this;
    }

    /**
     * Setter for kubeClient
     *
     * @param kubeClient kubeClientInstance
     * @return {@link LogCollectorBuilder} object
     */
    public LogCollectorBuilder withKubeClient(KubeClient kubeClient) {
        this.kubeClient = kubeClient;
        return this;
    }

    /**
     * Setter for kubeCmdClient
     *
     * @param kubeCmdClient kubeClientInstance
     * @return {@link LogCollectorBuilder} object
     */
    public LogCollectorBuilder withKubeCmdClient(KubeCmdClient<?> kubeCmdClient) {
        this.kubeCmdClient = kubeCmdClient;
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
     * Getter returning currently configured {@link #namespacedResources} in {@link List} object
     *
     * @return {@link #namespacedResources}
     */
    public List<String> getNamespacedResources() {
        return this.namespacedResources;
    }

    /**
     * Getter returning currently configured {@link #clusterWideResources} in {@link List} object
     *
     * @return {@link #clusterWideResources}
     */
    public List<String> getClusterWideResources() {
        return this.clusterWideResources;
    }

    /**
     * Getter for kubeClient
     *
     * @return {@link #kubeClient}
     */
    public KubeClient getKubeClient() {
        return this.kubeClient;
    }

    /**
     * Getter for kubeCmdClient
     *
     * @return {@link #kubeCmdClient}
     */
    public KubeCmdClient<?> getKubeCmdClient() {
        return this.kubeCmdClient;
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
