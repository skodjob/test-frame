/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.test.integration;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.skodjob.testframe.annotations.ResourceManager;
import io.skodjob.testframe.annotations.TestVisualSeparator;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ResourceManager
@TestVisualSeparator
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class KubeClientIT {

    @Test
    void testCreateResourcesFromYaml() throws IOException {
        List<HasMetadata> resoruces = KubeResourceManager.getKubeClient()
                .readResourcesFromFile(getClass().getClassLoader().getResourceAsStream("resources.yaml"));

        KubeResourceManager.getInstance().createResourceWithWait(resoruces.toArray(new HasMetadata[0]));

        assertNotNull(KubeResourceManager.getKubeClient().getClient().namespaces().withName("test4").get());
        assertNotNull(KubeResourceManager.getKubeClient().getClient().serviceAccounts()
                .inNamespace("test4").withName("skodjob-test-user").get());
    }
}
