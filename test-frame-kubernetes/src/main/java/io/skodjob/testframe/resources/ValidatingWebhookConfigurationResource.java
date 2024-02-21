package io.skodjob.testframe.resources;

import io.fabric8.kubernetes.api.model.admissionregistration.v1.ValidatingWebhookConfiguration;
import io.fabric8.kubernetes.api.model.admissionregistration.v1.ValidatingWebhookConfigurationList;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.clients.KubeClient;
import io.skodjob.testframe.interfaces.ResourceType;

import java.util.function.Consumer;

public class ValidatingWebhookConfigurationResource implements ResourceType<ValidatingWebhookConfiguration, ValidatingWebhookConfigurationList, Resource<ValidatingWebhookConfiguration>> {


    private final NonNamespaceOperation<ValidatingWebhookConfiguration, ValidatingWebhookConfigurationList, Resource<ValidatingWebhookConfiguration>> client;

    public ValidatingWebhookConfigurationResource() {
        this.client = KubeClient.getInstance().getClient().admissionRegistration().v1().validatingWebhookConfigurations();
    }

    /**
     * Returns client for resource {@link ValidatingWebhookConfiguration}
     *
     * @return client of a MixedOperation<{@link ValidatingWebhookConfiguration}, {@link ValidatingWebhookConfigurationList}, Resource<{@link ValidatingWebhookConfiguration}>> resource
     */
    @Override
    public NonNamespaceOperation<ValidatingWebhookConfiguration, ValidatingWebhookConfigurationList, Resource<ValidatingWebhookConfiguration>> getClient() {
        return client;
    }

    /**
     * Creates specific {@link ValidatingWebhookConfiguration} resource
     *
     * @param resource {@link ValidatingWebhookConfiguration} resource
     */
    @Override
    public void create(ValidatingWebhookConfiguration resource) {
        client.resource(resource).create();
    }

    /**
     * Updates specific {@link ValidatingWebhookConfiguration} resource
     *
     * @param resource {@link ValidatingWebhookConfiguration} resource that will be updated
     */
    @Override
    public void update(ValidatingWebhookConfiguration resource) {
        client.resource(resource).update();
    }

    /**
     * Deletes {@link ValidatingWebhookConfiguration} resource from Namespace in current context
     *
     * @param resourceName name of the {@link ValidatingWebhookConfiguration} that will be deleted
     */
    @Override
    public void delete(String resourceName) {
        client.withName(resourceName).delete();
    }

    /**
     * Replaces {@link ValidatingWebhookConfiguration} resource using {@link Consumer}
     * from which is the current {@link ValidatingWebhookConfiguration} resource updated
     *
     * @param resourceName name of the {@link ValidatingWebhookConfiguration} that will be replaced
     * @param editor       {@link Consumer} containing updates to the resource
     */
    @Override
    public void replace(String resourceName, Consumer<ValidatingWebhookConfiguration> editor) {
        ValidatingWebhookConfiguration toBeReplaced = client.withName(resourceName).get();
        editor.accept(toBeReplaced);
        update(toBeReplaced);
    }

    /**
     * Waits for {@link ValidatingWebhookConfiguration} to be ready (created/running)
     *
     * @param resource
     * @return result of the readiness check
     */
    @Override
    public boolean waitForReadiness(ValidatingWebhookConfiguration resource) {
        return resource != null;
    }

    /**
     * Waits for {@link ValidatingWebhookConfiguration} to be deleted
     *
     * @param resource
     * @return result of the deletion
     */
    @Override
    public boolean waitForDeletion(ValidatingWebhookConfiguration resource) {
        return resource == null;
    }
}
