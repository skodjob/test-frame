/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.skodjob.testframe.clients.cmdClient;

import java.io.File;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.skodjob.testframe.clients.KubeClusterException;
import io.skodjob.testframe.executor.Exec;
import io.skodjob.testframe.executor.ExecResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static java.lang.String.join;
import static java.util.Arrays.asList;

/**
 * Abstract class representing a base Kubernetes command-line client.
 *
 * @param <K> The subtype of KubeClient, for fluency.
 */
public abstract class BaseCmdKubeClient<K extends BaseCmdKubeClient<K>> implements KubeCmdClient<K> {

    private static final Logger LOGGER = LogManager.getLogger(BaseCmdKubeClient.class);

    private static final String CREATE = "create";
    private static final String APPLY = "apply";
    private static final String DELETE = "delete";
    private static final String REPLACE = "replace";
    private static final String PROCESS = "process";
    private static final String GET = "get";

    protected String config;

    String namespace = defaultNamespace();

    /**
     * Constructor for BaseCmdKubeClient.
     *
     * @param config The Kubernetes configuration file path.
     */
    protected BaseCmdKubeClient(String config) {
        this.config = config;
    }

    /**
     * Abstract method to retrieve the command.
     *
     * @return The command string.
     */
    @Override
    public abstract String cmd();

    /**
     * Deletes a resource by its name.
     *
     * @param resourceType The type of the resource.
     * @param resourceName The name of the resource.
     * @return The instance of the client.
     */
    @Override
    @SuppressWarnings("unchecked")
    public K deleteByName(String resourceType, String resourceName) {
        Exec.exec(namespacedCommand(DELETE, resourceType, resourceName));
        return (K) this;
    }

    protected static class Context implements AutoCloseable {
        @Override
        public void close() {
        }
    }

    private static final Context NOOP = new Context();

    protected Context defaultContext() {
        return NOOP;
    }

    // Admin context is not implemented now, because it's not needed
    // In case it will be needed in the future, we should change the kubeconfig and apply it for both oc and kubectl
    protected Context adminContext() {
        return defaultContext();
    }

    protected List<String> namespacedCommand(String... rest) {
        List<String> cmd = new ArrayList<>(List.of("--namespace", namespace));
        cmd.addAll(asList(rest));

        return command(cmd);
    }

    /**
     * Retrieves the YAML representation of a resource.
     *
     * @param resource     The type of the resource.
     * @param resourceName The name of the resource.
     * @return The YAML representation of the resource.
     */
    @Override
    public String get(String resource, String resourceName) {
        return Exec.exec(namespacedCommand(GET, resource, resourceName, "-o", "yaml")).out();
    }

    /**
     * Retrieves the events from the cluster.
     *
     * @return The events as a string.
     */
    @Override
    public String getEvents() {
        return Exec.exec(namespacedCommand(GET, "events")).out();
    }

    /**
     * Creates resources from files.
     *
     * @param files The files representing the resources.
     * @return The instance of the client.
     */
    @Override
    @SuppressWarnings("unchecked")
    public K create(File... files) {
        try (Context context = defaultContext()) {
            Map<File, ExecResult> execResults = execRecursive(CREATE, files, Comparator.comparing(File::getName)
                .reversed());
            for (Map.Entry<File, ExecResult> entry : execResults.entrySet()) {
                if (!entry.getValue().exitStatus()) {
                    LOGGER.warn("Failed to create {}!", entry.getKey().getAbsolutePath());
                    LOGGER.debug(entry.getValue().err());
                }
            }
            return (K) this;
        }
    }

    /**
     * Applies resources from files.
     *
     * @param files The files representing the resources.
     * @return The instance of the client.
     */
    @Override
    @SuppressWarnings("unchecked")
    public K apply(File... files) {
        try (Context context = defaultContext()) {
            Map<File, ExecResult> execResults = execRecursive(APPLY, files, Comparator.comparing(File::getName)
                .reversed());
            for (Map.Entry<File, ExecResult> entry : execResults.entrySet()) {
                if (!entry.getValue().exitStatus()) {
                    LOGGER.warn("Failed to apply {}!", entry.getKey().getAbsolutePath());
                    LOGGER.debug(entry.getValue().err());
                }
            }
            return (K) this;
        }
    }

    /**
     * Deletes resources from files.
     *
     * @param files The files representing the resources.
     * @return The instance of the client.
     */
    @Override
    @SuppressWarnings("unchecked")
    public K delete(File... files) {
        try (Context context = defaultContext()) {
            Map<File, ExecResult> execResults = execRecursive(DELETE, files, Comparator.comparing(File::getName)
                .reversed());
            for (Map.Entry<File, ExecResult> entry : execResults.entrySet()) {
                if (!entry.getValue().exitStatus()) {
                    LOGGER.warn("Failed to delete {}!", entry.getKey().getAbsolutePath());
                    LOGGER.debug(entry.getValue().err());
                }
            }
            return (K) this;
        }
    }

    private Map<File, ExecResult> execRecursive(String subcommand, File[] files, Comparator<File> cmp) {
        Map<File, ExecResult> execResults = new HashMap<>(25);
        for (File f : files) {
            if (f.isFile()) {
                if (f.getName().endsWith(".yaml")) {
                    execResults.put(f, Exec.exec(null, namespacedCommand(subcommand, "-f",
                        f.getAbsolutePath()), 0, false, false));
                }
            } else if (f.isDirectory()) {
                File[] children = f.listFiles();
                if (children != null) {
                    Arrays.sort(children, cmp);
                    execResults.putAll(execRecursive(subcommand, children, cmp));
                }
            } else if (!f.exists()) {
                throw new RuntimeException(new NoSuchFileException(f.getPath()));
            }
        }
        return execResults;
    }

    /**
     * Replaces resources from files.
     *
     * @param files The files representing the resources.
     * @return The instance of the client.
     */
    @Override
    @SuppressWarnings("unchecked")
    public K replace(File... files) {
        try (Context context = defaultContext()) {
            Map<File, ExecResult> execResults = execRecursive(REPLACE, files, Comparator.comparing(File::getName));
            for (Map.Entry<File, ExecResult> entry : execResults.entrySet()) {
                if (!entry.getValue().exitStatus()) {
                    LOGGER.warn("Failed to replace {}!", entry.getKey().getAbsolutePath());
                    LOGGER.debug(entry.getValue().err());
                }
            }
            return (K) this;
        }
    }

    /**
     * Applies YAML content in the current namespace.
     *
     * @param yamlContent The YAML content.
     * @return The instance of the client.
     */
    @Override
    @SuppressWarnings("unchecked")
    public K applyContentInNamespace(String yamlContent) {
        try (Context context = defaultContext()) {
            Exec.exec(yamlContent, namespacedCommand(APPLY, "-f", "-"));
            return (K) this;
        }
    }

    /**
     * Deletes YAML content in the current namespace.
     *
     * @param yamlContent The YAML content.
     * @return The instance of the client.
     */
    @Override
    @SuppressWarnings("unchecked")
    public K deleteContentInNamespace(String yamlContent) {
        try (Context context = defaultContext()) {
            Exec.exec(yamlContent, namespacedCommand(DELETE, "-f", "-"), 0, true, false);
            return (K) this;
        }
    }

    /**
     * Applies YAML content.
     *
     * @param yamlContent The YAML content.
     * @return The instance of the client.
     */
    @Override
    @SuppressWarnings("unchecked")
    public K applyContent(String yamlContent) {
        try (Context context = defaultContext()) {
            Exec.exec(yamlContent, command(Arrays.asList(APPLY, "-f", "-")), 0,
                true, true);
            return (K) this;
        }
    }

    /**
     * Deletes YAML content.
     *
     * @param yamlContent The YAML content.
     * @return The instance of the client.
     */
    @Override
    @SuppressWarnings("unchecked")
    public K deleteContent(String yamlContent) {
        try (Context context = defaultContext()) {
            Exec.exec(yamlContent, command(Arrays.asList(DELETE, "-f", "-")), 0,
                true, false);
            return (K) this;
        }
    }

    /**
     * Creates a namespace.
     *
     * @param name The name of the namespace.
     * @return The instance of the client.
     */
    @Override
    @SuppressWarnings("unchecked")
    public K createNamespace(String name) {
        try (Context context = adminContext()) {
            Exec.exec(namespacedCommand(CREATE, "namespace", name));
        }
        return (K) this;
    }

    /**
     * Deletes a namespace.
     *
     * @param name The name of the namespace.
     * @return The instance of the client.
     */
    @Override
    @SuppressWarnings("unchecked")
    public K deleteNamespace(String name) {
        try (Context context = adminContext()) {
            Exec.exec(null, namespacedCommand(DELETE, "namespace", name), 0, true, false);
        }
        return (K) this;
    }

    /**
     * Scales resources by name.
     *
     * @param kind     The kind of the resource.
     * @param name     The name of the resource.
     * @param replicas The number of replicas.
     * @return The instance of the client.
     */
    @Override
    @SuppressWarnings("unchecked")
    public K scaleByName(String kind, String name, int replicas) {
        try (Context context = defaultContext()) {
            Exec.exec(null, namespacedCommand("scale", kind, name, "--replicas", Integer.toString(replicas)));
            return (K) this;
        }
    }

    /**
     * Executes a command in a pod.
     *
     * @param pod     The name of the pod.
     * @param command The command to execute.
     * @return The execution result.
     */
    @Override
    public ExecResult execInPod(String pod, String... command) {
        List<String> cmd = namespacedCommand("exec", pod, "--");
        cmd.addAll(asList(command));
        return Exec.exec(cmd);
    }

    /**
     * Executes a command in a pod container.
     *
     * @param pod       The name of the pod.
     * @param container The name of the container.
     * @param command   The command to execute.
     * @return The execution result.
     */
    @Override
    public ExecResult execInPodContainer(String pod, String container, String... command) {
        return execInPodContainer(true, pod, container, command);
    }

    /**
     * Executes a command in a pod container with the option to log to output.
     *
     * @param logToOutput Whether to log the output.
     * @param pod         The name of the pod.
     * @param container   The name of the container.
     * @param command     The command to execute.
     * @return The execution result.
     */
    @Override
    public ExecResult execInPodContainer(boolean logToOutput, String pod, String container, String... command) {
        List<String> cmd = namespacedCommand("exec", pod, "-c", container, "--");
        cmd.addAll(asList(command));
        return Exec.exec(null, cmd, 0, logToOutput);
    }

    /**
     * Executes a command.
     *
     * @param command The command to execute.
     * @return The execution result.
     */
    @Override
    public ExecResult exec(String... command) {
        return exec(true, command);
    }

    /**
     * Executes a command with the option to throw errors.
     *
     * @param throwError Whether to throw errors.
     * @param command    The command to execute.
     * @return The execution result.
     */
    @Override
    public ExecResult exec(boolean throwError, String... command) {
        return exec(throwError, true, command);
    }

    /**
     * Executes a command with the option to throw errors and log to output.
     *
     * @param throwError  Whether to throw errors.
     * @param logToOutput Whether to log the output.
     * @param command     The command to execute.
     * @return The execution result.
     */
    @Override
    public ExecResult exec(boolean throwError, boolean logToOutput, String... command) {
        List<String> cmd = command(asList(command));
        return Exec.exec(null, cmd, 0, logToOutput, throwError);
    }

    /**
     * Executes a command with the option to throw errors and log to output.
     *
     * @param throwError  Whether to throw errors.
     * @param logToOutput Whether to log the output.
     * @param timeout     timeout in milis
     * @param command     The command to execute.
     * @return The execution result.
     */
    @Override
    public ExecResult exec(boolean throwError, boolean logToOutput, int timeout, String... command) {
        List<String> cmd = command(asList(command));
        return Exec.exec(null, cmd, timeout, logToOutput, throwError);
    }

    /**
     * Executes a command in the current namespace.
     *
     * @param commands The commands to execute.
     * @return The execution result.
     */
    @Override
    public ExecResult execInCurrentNamespace(String... commands) {
        return Exec.exec(namespacedCommand(commands));
    }

    /**
     * Executes a command in the current namespace with the option to log to output.
     *
     * @param logToOutput Whether to log the output.
     * @param commands    The commands to execute.
     * @return The execution result.
     */
    @Override
    public ExecResult execInCurrentNamespace(boolean logToOutput, String... commands) {
        return Exec.exec(null, namespacedCommand(commands), 0, logToOutput);
    }

    /**
     * Converts the object to a string representation.
     *
     * @return The string representation of the object.
     */
    @Override
    public String toString() {
        return cmd();
    }

    /**
     * Lists resources of a certain type.
     *
     * @param resourceType The type of the resource.
     * @return A list of resource names.
     */
    @Override
    public List<String> list(String resourceType) {
        return Arrays.stream(Exec.exec(namespacedCommand(GET, resourceType,
                    "-o", "jsonpath={range .items[*]}{.metadata.name} "))
                .out().trim().split(" +"))
            .filter(s -> !s.trim().isEmpty()).collect(Collectors.toList());
    }

    /**
     * Lists cluster wide resources of a certain type.
     *
     * @param resourceType The type of the resource.
     * @return A list of resource names.
     */
    @Override
    public List<String> listClusterWide(String resourceType) {
        return Arrays.stream(Exec.exec(command(List.of(GET, resourceType,
                    "-o", "jsonpath={range .items[*]}{.metadata.name} ")))
                .out().trim().split(" +"))
            .filter(s -> !s.trim().isEmpty()).collect(Collectors.toList());
    }

    /**
     * Retrieves a resource as JSON.
     *
     * @param resourceType The type of the resource.
     * @param resourceName The name of the resource.
     * @return The resource as JSON.
     */
    @Override
    public String getResourceAsJson(String resourceType, String resourceName) {
        return Exec.exec(namespacedCommand(GET, resourceType, resourceName, "-o", "json")).out();
    }

    /**
     * Retrieves a resource as YAML.
     *
     * @param resourceType The type of the resource.
     * @param resourceName The name of the resource.
     * @return The resource as YAML.
     */
    @Override
    public String getResourceAsYaml(String resourceType, String resourceName) {
        return Exec.exec(namespacedCommand(GET, resourceType, resourceName, "-o", "yaml")).out();
    }

    /**
     * Retrieves resources as YAML.
     *
     * @param resourceType The type of the resource.
     * @return The resources as YAML.
     */
    @Override
    public String getResourcesAsYaml(String resourceType) {
        return Exec.exec(namespacedCommand(GET, resourceType, "-o", "yaml")).out();
    }

    /**
     * Retrieves a  cluster wide resource as YAML.
     *
     * @param resourceType The type of the resource.
     * @param resourceName The name of the resource.
     * @return The resource as YAML.
     */
    @Override
    public String getClusterWideResourceAsYaml(String resourceType, String resourceName) {
        return Exec.exec(command(List.of(GET, resourceType, resourceName, "-o", "yaml"))).out();
    }

    /**
     * Retrieves resources as YAML.
     *
     * @param resourceType The type of the resource.
     * @return The resources as YAML.
     */
    @Override
    public String getClusterWideResourcesAsYaml(String resourceType) {
        return Exec.exec(command(List.of(GET, resourceType, "-o", "yaml"))).out();
    }

    /**
     * Creates a resource from a template and applies it.
     *
     * @param template The template file.
     * @param params   The parameters for the template.
     */
    @Override
    public void createResourceAndApply(String template, Map<String, String> params) {
        List<String> cmd = namespacedCommand("process", template, "-l", "app=" + template, "-o", "yaml");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            cmd.add("-p");
            cmd.add(entry.getKey() + "=" + entry.getValue());
        }

        String yaml = Exec.exec(cmd).out();
        applyContentInNamespace(yaml);
    }

    /**
     * Retrieves the description of a resource.
     *
     * @param resourceType The type of the resource.
     * @param resourceName The name of the resource.
     * @return The description of the resource.
     */
    @Override
    public String describe(String resourceType, String resourceName) {
        return Exec.exec(namespacedCommand("describe", resourceType, resourceName)).out();
    }

    /**
     * Retrieves logs of a pod/container.
     *
     * @param pod       The name of the pod.
     * @param container The name of the container.
     * @return The logs as a string.
     */
    @Override
    public String logs(String pod, String container) {
        String[] args;
        if (container != null) {
            args = new String[]{"logs", pod, "-c", container};
        } else {
            args = new String[]{"logs", pod};
        }
        return Exec.exec(namespacedCommand(args)).out();
    }

    /**
     * Searches for patterns in logs.
     *
     * @param resourceType The type of the resource.
     * @param resourceName The name of the resource.
     * @param sinceSeconds The duration since the logs should be searched.
     * @param grepPattern  The patterns to search for.
     * @return The matching lines from logs.
     */
    @Override
    public String searchInLog(String resourceType, String resourceName, long sinceSeconds, String... grepPattern) {
        try {
            return Exec.exec("bash", "-c", join(" ",
                    namespacedCommand("logs", resourceType + "/" + resourceName,
                        "--since=" + sinceSeconds + "s",
                        "|", "grep", " -e " + join(" -e ", grepPattern), "-B", "1")))
                .out();
        } catch (KubeClusterException e) {
            if (e.result != null && e.result.returnCode() == 1) {
                LOGGER.info("{} not found", Arrays.stream(grepPattern).toList());
            } else {
                LOGGER.error("Caught exception while searching {} in logs", Arrays.stream(grepPattern).toList());
            }
        }
        return "";
    }

    /**
     * Searches for patterns in logs of a specific container.
     *
     * @param resourceType      The type of the resource.
     * @param resourceName      The name of the resource.
     * @param resourceContainer The name of the container.
     * @param sinceSeconds      The duration since the logs should be searched.
     * @param grepPattern       The patterns to search for.
     * @return The matching lines from logs.
     */
    @Override
    public String searchInLog(String resourceType, String resourceName, String resourceContainer,
                              long sinceSeconds, String... grepPattern) {
        try {
            return Exec.exec("bash", "-c", join(" ", namespacedCommand("logs",
                resourceType + "/" + resourceName, "-c " + resourceContainer, "--since=" + sinceSeconds + "s",
                "|", "grep", " -e " + join(" -e ", grepPattern), "-B", "1"))).out();
        } catch (KubeClusterException e) {
            if (e.result != null && e.result.exitStatus()) {
                LOGGER.info("{} not found", Arrays.stream(grepPattern).toList());
            } else {
                LOGGER.error("Caught exception while searching {} in logs", Arrays.stream(grepPattern).toList());
            }
        }
        return "";
    }

    /**
     * Lists resources by label.
     *
     * @param resourceType The type of the resource.
     * @param label        The label.
     * @return A list of resource names.
     */
    @Override
    public List<String> listResourcesByLabel(String resourceType, String label) {
        return asList(Exec.exec(namespacedCommand(GET, resourceType,
                "-l", label, "-o", "jsonpath={range .items[*]}{.metadata.name} "))
            .out().split("\\s+"));
    }

    private List<String> command(List<String> rest) {
        List<String> result = new ArrayList<>();
        result.add(cmd());

        if (config != null) {
            result.add("--kubeconfig");
            result.add(config);
        }

        result.addAll(rest);
        return result;
    }

    /**
     * Processes a template file with parameters.
     *
     * @param parameters The parameters for the template.
     * @param file       The template file.
     * @param c          The consumer to process the output.
     * @return The instance of the client.
     */
    @Override
    @SuppressWarnings("unchecked")
    public K process(Map<String, String> parameters, String file, Consumer<String> c) {
        List<String> command = command(asList(PROCESS, "-f", file));
        command.addAll(parameters.entrySet().stream()
            .map(e -> "-p " + e.getKey() + "=" + e.getValue())
            .toList());

        c.accept(Exec.exec(null, command, 0, false).out());
        return (K) this;
    }
}
