/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.clients.cmdClient;

/**
 * A {@link KubeCmdClient} wrapping {@code kubectl}.
 */
public class Kubectl extends BaseCmdKubeClient<Kubectl> {

    public static final String KUBECTL = "kubectl";

    public Kubectl() {
        this(null);
    }

    public Kubectl(String config) {
        super(config);
    }

    private Kubectl(String futureNamespace, String config) {
        super(config);
        namespace = futureNamespace;
    }

    @Override
    public Kubectl namespace(String namespace) {
        return new Kubectl(namespace, config);
    }

    @Override
    public String namespace() {
        return namespace;
    }

    @Override
    public String defaultNamespace() {
        return "default";
    }

    @Override
    public String defaultOlmNamespace() {
        return "operators";
    }

    @Override
    public String cmd() {
        return KUBECTL;
    }

    @Override
    public String getUsername() {
        // TODO - implement this!
        return null;
    }
}
