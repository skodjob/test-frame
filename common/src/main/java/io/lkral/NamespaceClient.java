package io.lkral;

import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

public class NamespaceClient {
    private final NonNamespaceOperation<Namespace, NamespaceList, Resource<Namespace>> namespaceClient;
    private static NamespaceClient client;

    private NamespaceClient() {
        this.namespaceClient = KubeClient.getInstance().getClient().namespaces();
    }

    public static NamespaceClient client() {
        if (client == null) {
            client = new NamespaceClient();
        }

        return client;
    }

    public NonNamespaceOperation<Namespace, NamespaceList, Resource<Namespace>> getClient() {
        return namespaceClient;
    }

    /**
     * Method for creating Namespace with specific name
     * @param namespaceName name of the namespace
     */
    public void create(String namespaceName) {
        namespaceClient.withName(namespaceName).create();
    }

    /**
     * Method for creating namespace with specific configuration
     * @param namespace Namespace resource
     */
    public void create(Namespace namespace) {
        namespaceClient.resource(namespace).create();
    }

    /**
     * Method for getting the Namespace object with specific name
     * @param namespaceName Namespace name
     * @return Namespace object
     */
    public Namespace get(String namespaceName) {
        return namespaceClient.withName(namespaceName).get();
    }

    /**
     * Method for updating the Namespace object with new configuration
     * @param namespace updated Namespace object
     */
    public void update(Namespace namespace) {
        namespaceClient.resource(namespace).update();
    }

    /**
     * Method for deleting Namespace with specific name
     * @param namespaceName Namespace name
     */
    public void delete(String namespaceName) {
        namespaceClient.withName(namespaceName).delete();
    }
}
