/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.test.integration;

import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.skodjob.testframe.annotations.ResourceManager;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ResourceManager(cleanResources = false) // override default behavior and do not clean resources
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class KubeResourceManagerIT extends AbstractIT {

    @BeforeEach
    void setupEach() {
        KubeResourceManager.getInstance().createResourceWithWait(
                new NamespaceBuilder().withNewMetadata().withName(nsName1).endMetadata().build());
    }

    @AfterEach
    void afterEach() {
        assertNotNull(KubeResourceManager.getKubeClient().getClient().namespaces().withName(nsName1).get());
        assertNotNull(KubeResourceManager.getKubeClient().getClient().namespaces().withName(nsName2).get());
        KubeResourceManager.getInstance().deleteResources();
    }

    @Test
    void createResource() {
        KubeResourceManager.getInstance().createResourceWithWait(
                new NamespaceBuilder().withNewMetadata().withName("test2").endMetadata().build());
        assertNotNull(KubeResourceManager.getKubeClient().getClient().namespaces().withName(nsName1).get());
        assertNotNull(KubeResourceManager.getKubeClient().getClient().namespaces().withName(nsName2).get());
    }
}
