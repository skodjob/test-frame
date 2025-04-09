/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.test.integration;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.skodjob.testframe.annotations.ResourceManager;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.resources.NamespaceType;
import org.junit.jupiter.api.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ResourceManager(cleanResources = false) // override default behavior and do not clean resources
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class KubeResourceManagerIT extends AbstractIT {
    Namespace ns1 = new NamespaceBuilder().withNewMetadata().withName(nsName1).endMetadata().build();

    @BeforeAll
    void setupAll() {
        KubeResourceManager.get().createResourceWithWait(ns1);
    }

    @AfterEach
    void afterEach() {
        Namespace ns2 = KubeResourceManager.get().kubeClient().getClient().namespaces().withName(nsName2).get();
        assertNotNull(ns2);
        KubeResourceManager.get().deleteResource(false, ns2);
        assertTrue(isDeleteHandlerCalled.get());
    }

    @AfterAll
    void afterAll() {
        assertNotNull(KubeResourceManager.get().kubeClient().getClient().namespaces().withName(nsName1).get());
        KubeResourceManager.get().deleteResources();
    }


    @Test
    void createResource() {
        Namespace ns2 = new NamespaceBuilder().withNewMetadata().withName(nsName2).endMetadata().build();
        KubeResourceManager.get().createResourceWithWait(ns2);
        assertNotNull(KubeResourceManager.get().kubeClient().getClient().namespaces().withName(nsName1).get());
        assertNotNull(KubeResourceManager.get().kubeClient().getClient().namespaces().withName(nsName2).get());
    }

    @Test
    void replaceResource() {
        Namespace ns = new NamespaceBuilder().withNewMetadata().withName(nsName2).endMetadata().build();
        KubeResourceManager.get().createResourceWithWait(ns);
        assertNotNull(KubeResourceManager.get().kubeClient().getClient().namespaces().withName(nsName2).get());

        KubeResourceManager.get().replaceResource(ns,
            resource -> resource.getMetadata().setLabels(Map.of("my-label", "here")));
        assertNotNull(KubeResourceManager.get().kubeClient().getClient().namespaces().withName(nsName2).get()
            .getMetadata().getLabels().get("my-label"));
    }
}
