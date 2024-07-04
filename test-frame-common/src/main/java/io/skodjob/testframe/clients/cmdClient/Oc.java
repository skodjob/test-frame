/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.clients.cmdClient;

import java.util.List;
import java.util.Map;

import io.skodjob.testframe.executor.Exec;

/**
 * A {@link KubeCmdClient} implementation wrapping {@code oc}.
 */
public class Oc extends BaseCmdKubeClient<Oc> {

    /**
     * The command name for oc.
     */
    private static final String OC = "oc";

    /**
     * Constructs a new Oc instance.
     */
    public Oc() {
        this(null);
    }

    /**
     * Constructs a new Oc instance with the specified configuration.
     *
     * @param config The configuration to use.
     */
    public Oc(String config) {
        super(config);
    }

    private Oc(String futureNamespace, String config) {
        super(config);
        namespace = futureNamespace;
    }

    /**
     * Gets the default OLM (Operator Lifecycle Manager) namespace.
     *
     * @return The default OLM namespace.
     */
    @Override
    public String defaultOlmNamespace() {
        return "openshift-marketplace";
    }

    /**
     * Sets the namespace for the Oc instance.
     *
     * @param namespace The namespace to set.
     * @return A new Oc instance with the specified namespace.
     */
    @Override
    public Oc inNamespace(String namespace) {
        return new Oc(namespace, config);
    }

    /**
     * Gets the current namespace of the Oc instance.
     *
     * @return The current namespace.
     */
    @Override
    public String getCurrentNamespace() {
        return namespace;
    }

    /**
     * Creates a new namespace.
     *
     * @param name The name of the namespace to create.
     * @return The Oc instance.
     */
    @Override
    public Oc createNamespace(String name) {
        try (Context context = defaultContext()) {
            Exec.exec(cmd(), "new-project", name);
        }
        return this;
    }

    /**
     * Creates a new application.
     *
     * @param template The template file.
     * @param params   The parameters for the template.
     * @return The Oc instance.
     */
    public Oc newApp(String template, Map<String, String> params) {
        List<String> cmd = command("new-app", template);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            cmd.add("-p");
            cmd.add(entry.getKey() + "=" + entry.getValue());
        }

        Exec.exec(cmd);
        return this;
    }

    /**
     * Gets the command name for oc.
     *
     * @return The command name.
     */
    @Override
    public String cmd() {
        return OC;
    }

    /**
     * Gets the username.
     *
     * @return The username.
     */
    @Override
    public String getUsername() {
        return Exec.exec(cmd(), "whoami").out();
    }
}
