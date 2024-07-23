/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.clients;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.skodjob.testframe.annotations.ResourceManager;
import io.skodjob.testframe.annotations.TestVisualSeparator;
import io.skodjob.testframe.helper.TestLoggerAppender;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


@EnableKubernetesMockClient(crud = true)
@ResourceManager
@TestVisualSeparator
public class KubeResourceManagerTest {
    private KubernetesClient kubernetesClient;
    private KubernetesMockServer server;

    @BeforeEach
    void setupClient() {
        KubeResourceManager.getKubeClient().testReconnect(kubernetesClient.getConfiguration());
    }

    @Test
    void testCreateDeleteNamespace() {
        KubeResourceManager.getInstance().createResourceWithWait(
            new NamespaceBuilder().withNewMetadata().withName("test").endMetadata().build());
        assertNotNull(KubeResourceManager.getKubeClient().getClient().namespaces().withName("test").get());
    }

    @Test
    void testDeleteAllResources() {
        KubeResourceManager.getInstance().createResourceWithWait(
            new NamespaceBuilder().withNewMetadata().withName("test2").endMetadata().build());
        assertNull(KubeResourceManager.getKubeClient().getClient().namespaces().withName("test").get());
        assertNotNull(KubeResourceManager.getKubeClient().getClient().namespaces().withName("test2").get());
        KubeResourceManager.getInstance().deleteResources();
        assertNull(KubeResourceManager.getKubeClient().getClient().namespaces().withName("test2").get());
    }

    @Test
    void testUpdateResource() {
        Namespace ns = new NamespaceBuilder().withNewMetadata().withName("test3").endMetadata().build();
        KubeResourceManager.getInstance().createResourceWithWait(ns);
        assertNotNull(KubeResourceManager.getKubeClient().getClient().namespaces().withName("test3").get());
        KubeResourceManager.getInstance().updateResource(ns.edit()
            .editMetadata().addToLabels(Collections.singletonMap("test-label", "true")).endMetadata().build());
        assertNotNull(KubeResourceManager.getKubeClient().getClient().namespaces().withName("test3").get()
            .getMetadata().getLabels().get("test-label"));
    }

    @Test
    void testCreateOrUpdateResource() {
        Namespace ns = new NamespaceBuilder().withNewMetadata().withName("test4").endMetadata().build();
        KubeResourceManager.getInstance().createResourceWithWait(ns);
        assertNotNull(KubeResourceManager.getKubeClient().getClient().namespaces().withName("test4").get());
        KubeResourceManager.getInstance().createOrUpdateResourceWithWait(ns);
        assertNotNull(KubeResourceManager.getKubeClient().getClient().namespaces().withName("test4").get());
        KubeResourceManager.getInstance().createOrUpdateResourceWithoutWait(ns);
        assertNotNull(KubeResourceManager.getKubeClient().getClient().namespaces().withName("test4").get());
    }

    @Test
    void testLoggingManagedResources() {
        // create resources
        KubeResourceManager.getInstance().createResourceWithWait(
            new NamespaceBuilder().withNewMetadata().withName("test-ns").endMetadata().build());
        KubeResourceManager.getInstance().createResourceWithWait(
            new ServiceAccountBuilder().withNewMetadata().withName("test-sa").endMetadata().build());

        // setup mock logger appender
        TestLoggerAppender testAppender = new TestLoggerAppender("TestAppender");

        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration config = context.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        loggerConfig.addAppender(testAppender, null, null);
        Configurator.setLevel(loggerConfig.getName(), Level.DEBUG);
        testAppender.start();

        // print resources
        KubeResourceManager.getInstance().printCurrentResources(org.slf4j.event.Level.INFO);
        System.out.println(testAppender.getLogEvents());
        List<LogEvent> events = testAppender.getLogEvents();

        assertEquals(3, events.size());
        assertEquals("Managed resource: Namespace/test-ns", events.get(1).getMessage().getFormattedMessage());
        assertEquals("Managed resource: ServiceAccount/test-sa", events.get(2).getMessage().getFormattedMessage());
        assertEquals(Level.INFO, events.get(0).getLevel());

        testAppender.clean();

        // print all resources on debug output
        KubeResourceManager.getInstance().printAllResources(org.slf4j.event.Level.DEBUG);
        events = testAppender.getLogEvents();

        assertEquals(4, events.size());
        assertEquals("Managed resource: Namespace/test-ns", events.get(2).getMessage().getFormattedMessage());
        assertEquals("Managed resource: ServiceAccount/test-sa", events.get(3).getMessage().getFormattedMessage());
        assertEquals(Level.DEBUG, events.get(0).getLevel());
    }
}
