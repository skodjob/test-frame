/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.clients.cmdClient;

/**
 * A {@link KubeCmdClient} wrapping {@code kubectl}.
 */
public class Kubectl extends BaseCmdKubeClient<Kubectl> {

    /**
     * The command name for kubectl.
     */
    public static final String KUBECTL = "kubectl";

    /**
     * Constructs a new Kubectl instance.
     */
    public Kubectl() {
        this(null);
    }

    /**
     * Constructs a new Kubectl instance with the specified configuration.
     *
     * @param config The configuration to use.
     */
    public Kubectl(String config) {
        super(config);
    }

    private Kubectl(String futureNamespace, String config) {
        super(config);
        namespace = futureNamespace;
    }

    /**
     * Sets the namespace for the Kubectl instance.
     *
     * @param namespace The namespace to set.
     * @return A new Kubectl instance with the specified namespace.
     */
    @Override
    public Kubectl inNamespace(String namespace) {
        return new Kubectl(namespace, config);
    }

    /**
     * Gets the current namespace of the Kubectl instance.
     *
     * @return The current namespace.
     */
    @Override
    public String getCurrentNamespace() {
        return namespace;
    }

    /**
     * Gets the default namespace.
     *
     * @return The default namespace.
     */
    @Override
    public String defaultNamespace() {
        return "default";
    }

    /**
     * Gets the default OLM (Operator Lifecycle Manager) namespace.
     *
     * @return The default OLM namespace.
     */
    @Override
    public String defaultOlmNamespace() {
        return "operators";
    }

    /**
     * Gets the command name for kubectl.
     *
     * @return The command name.
     */
    @Override
    public String cmd() {
        return KUBECTL;
    }

    /**
     * Gets the username.
     *
     * @return The username.
     */
    @Override
    public String getUsername() {
        // TODO - implement this!
        return null;
    }
}
