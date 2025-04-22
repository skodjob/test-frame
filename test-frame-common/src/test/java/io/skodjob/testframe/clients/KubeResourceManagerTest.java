/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.clients;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.skodjob.testframe.TestFrameConstants;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


@EnableKubernetesMockClient(crud = true)
@ResourceManager
@TestVisualSeparator
class KubeResourceManagerTest {
    private KubernetesClient kubernetesClient;
    private KubernetesMockServer server;

    @BeforeEach
    void setupClient() {
        KubeResourceManager.get().kubeClient().testReconnect(kubernetesClient.getConfiguration());
    }

    @Test
    void testCreateDeleteNamespace() {
        KubeResourceManager.get().createResourceWithWait(
            new NamespaceBuilder().withNewMetadata().withName("test").endMetadata().build());
        assertNotNull(KubeResourceManager.get().kubeClient().getClient().namespaces().withName("test").get());
    }

    @Test
    void testDeleteAllResources() {
        KubeResourceManager.get().createResourceWithWait(
            new NamespaceBuilder().withNewMetadata().withName("test2").endMetadata().build());
        assertNull(KubeResourceManager.get().kubeClient().getClient().namespaces().withName("test").get());
        assertNotNull(KubeResourceManager.get().kubeClient().getClient().namespaces().withName("test2").get());
        KubeResourceManager.get().deleteResources();
        assertNull(KubeResourceManager.get().kubeClient().getClient().namespaces().withName("test2").get());
    }

    @Test
    void testUpdateResource() {
        Namespace ns = new NamespaceBuilder().withNewMetadata().withName("test3").endMetadata().build();
        KubeResourceManager.get().createResourceWithWait(ns);
        assertNotNull(KubeResourceManager.get().kubeClient().getClient().namespaces().withName("test3").get());
        KubeResourceManager.get().updateResource(ns.edit()
            .editMetadata().addToLabels(Collections.singletonMap("test-label", "true")).endMetadata().build());
        assertNotNull(KubeResourceManager.get().kubeClient().getClient().namespaces().withName("test3").get()
            .getMetadata().getLabels().get("test-label"));
    }

    @Test
    void testCreateOrUpdateResource() {
        Namespace ns = new NamespaceBuilder().withNewMetadata().withName("test4").endMetadata().build();
        KubeResourceManager.get().createResourceWithWait(ns);
        assertNotNull(KubeResourceManager.get().kubeClient().getClient().namespaces().withName("test4").get());
        KubeResourceManager.get().createOrUpdateResourceWithWait(ns);
        assertNotNull(KubeResourceManager.get().kubeClient().getClient().namespaces().withName("test4").get());
        KubeResourceManager.get().createOrUpdateResourceWithoutWait(ns);
        assertNotNull(KubeResourceManager.get().kubeClient().getClient().namespaces().withName("test4").get());
    }

    @Test
    void testReplaceResource() {
        Namespace ns = new NamespaceBuilder().withNewMetadata().withName("test5").endMetadata().build();
        KubeResourceManager.get().createResourceWithWait(ns);
        assertNotNull(KubeResourceManager.get().kubeClient().getClient().namespaces().withName("test5").get());

        KubeResourceManager.get().replaceResource(ns,
            resource -> resource.getMetadata().setLabels(Map.of("my-label", "here")));
        assertNotNull(KubeResourceManager.get().kubeClient().getClient().namespaces().withName("test5").get()
            .getMetadata().getLabels().get("my-label"));
    }

    @Test
    void testReplaceResourceWithRetries() {
        String namespaceName = "test6";

        // Check if replace will be retried when there is conflict once
        server
            .expect()
            .put()
            .withPath("/api/v1/namespaces/" + namespaceName)
            .andReturn(409, "{\"message\":\"Conflict\"}")
            .once();

        Namespace ns = new NamespaceBuilder().withNewMetadata().withName(namespaceName).endMetadata().build();
        KubeResourceManager.get().createResourceWithWait(ns);
        assertNotNull(KubeResourceManager.get().kubeClient().getClient().namespaces().withName(namespaceName).get());

        KubeResourceManager.get().replaceResourceWithRetries(ns,
            resource -> resource.getMetadata().setLabels(Map.of("my-label", "here")));
        assertNotNull(KubeResourceManager.get().kubeClient().getClient().namespaces().withName(namespaceName).get()
            .getMetadata().getLabels().get("my-label"));

        // Check retries will fail when it reaches the max retries (default 3)
        server
            .expect()
            .put()
            .withPath("/api/v1/namespaces/" + namespaceName)
            .andReturn(409, "{\"message\":\"Conflict\"}")
            .times(3);

        assertThrows(RuntimeException.class, () -> KubeResourceManager.get().replaceResourceWithRetries(ns,
            resource -> resource.getMetadata().setLabels(Map.of("my-label2", "not-here"))));

        // Check retries will fail if the exception is not Conflict code
        server
            .expect()
            .put()
            .withPath("/api/v1/namespaces/" + namespaceName)
            .andReturn(404, "{\"message\":\"Not-Found\"}")
            .once();

        assertThrows(RuntimeException.class, () -> KubeResourceManager.get().replaceResourceWithRetries(ns,
            resource -> resource.getMetadata().setLabels(Map.of("my-label2", "not-here"))));
    }

    @Test
    void testReplaceResourceWithRetriesSpecifiedByUser() {
        String namespaceName = "test7";
        int maxRetries = 6;

        // Check if replace will be retried when there is conflict once
        server
            .expect()
            .put()
            .withPath("/api/v1/namespaces/" + namespaceName)
            .andReturn(409, "{\"message\":\"Conflict\"}")
            .once();

        Namespace ns = new NamespaceBuilder().withNewMetadata().withName(namespaceName).endMetadata().build();
        KubeResourceManager.get().createResourceWithWait(ns);
        assertNotNull(KubeResourceManager.get().kubeClient().getClient().namespaces().withName(namespaceName).get());

        KubeResourceManager.get().replaceResourceWithRetries(ns,
            resource -> resource.getMetadata().setLabels(Map.of("my-label", "here")), maxRetries);
        assertNotNull(KubeResourceManager.get().kubeClient().getClient().namespaces().withName(namespaceName).get()
            .getMetadata().getLabels().get("my-label"));

        // Check retries will fail when it reaches the max retries (default 3)
        server
            .expect()
            .put()
            .withPath("/api/v1/namespaces/" + namespaceName)
            .andReturn(409, "{\"message\":\"Conflict\"}")
            .times(5);

        KubeResourceManager.get().replaceResourceWithRetries(ns,
            resource -> resource.getMetadata().setLabels(Map.of("my-label2", "not-here")), maxRetries);
        assertNotNull(KubeResourceManager.get().kubeClient().getClient().namespaces().withName(namespaceName).get()
            .getMetadata().getLabels().get("my-label2"));

        // Check retries will fail if the exception is not Conflict code
        server
            .expect()
            .put()
            .withPath("/api/v1/namespaces/" + namespaceName)
            .andReturn(404, "{\"message\":\"Not-Found\"}")
            .once();

        assertThrows(RuntimeException.class, () -> KubeResourceManager.get().replaceResourceWithRetries(ns,
            resource -> resource.getMetadata().setLabels(Map.of("my-label2", "not-here")), maxRetries));
    }

    @Test
    void testLoggingManagedResources() {
        // create resources
        KubeResourceManager.get().createResourceWithWait(
            new NamespaceBuilder().withNewMetadata().withName("test-ns").endMetadata().build());
        KubeResourceManager.get().createResourceWithWait(
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
        KubeResourceManager.get().printCurrentResources(org.slf4j.event.Level.INFO);
        System.out.println(testAppender.getLogEvents());
        List<LogEvent> events = testAppender.getLogEvents();

        assertEquals(3, events.size());
        assertEquals("Managed resource: Namespace/test-ns", events.get(1).getMessage().getFormattedMessage());
        assertEquals("Managed resource: ServiceAccount/test-sa", events.get(2).getMessage().getFormattedMessage());
        assertEquals(Level.INFO, events.get(0).getLevel());

        testAppender.clean();

        // print all resources on debug output
        KubeResourceManager.get().printAllResources(org.slf4j.event.Level.DEBUG);
        events = testAppender.getLogEvents();

        assertEquals(5, events.size());
        assertEquals("Managed resource: Namespace/test-ns", events.get(3).getMessage().getFormattedMessage());
        assertEquals("Managed resource: ServiceAccount/test-sa", events.get(4).getMessage().getFormattedMessage());
        assertEquals(Level.DEBUG, events.get(0).getLevel());
    }

    @Test
    void testUseContext() throws Exception {
        // create resources
        Namespace ns = new NamespaceBuilder().withNewMetadata().withName("test-ns-2").endMetadata().build();
        ServiceAccount sa = new ServiceAccountBuilder().withNewMetadata().withName("test-sa-2")
            .withNamespace("test-ns-2").endMetadata().build();

        KubeResourceManager.get().createResourceWithWait(ns);
        KubeResourceManager.get().createResourceWithWait(sa);

        try (var ignored = KubeResourceManager.get().useContext(TestFrameConstants.DEFAULT_CONTEXT_NAME)) {
            assertTrue(KubeResourceManager.get().kubeClient().namespaceExists("test-ns-2"));
            KubeResourceManager.get().deleteResource(sa);
        }

        assertNull(KubeResourceManager.get().kubeClient().getClient()
            .serviceAccounts().inNamespace("test-ns-2").withName("test-sa-2").get());
    }
}
