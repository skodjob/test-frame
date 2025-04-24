/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.clients;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.skodjob.testframe.annotations.TestVisualSeparator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;



@EnableKubernetesMockClient(crud = true)
@TestVisualSeparator
class KubeClientTest {
    private KubernetesClient kubernetesClient;
    private KubernetesMockServer server;

    @Test
    void testClientFromUrlAndToken() {
        KubeClient cl = KubeClient.fromUrlAndToken(kubernetesClient.getConfiguration().getMasterUrl(),
            kubernetesClient.getConfiguration().getOauthToken());
        assertNotEquals("", cl.getKubeconfigPath());
    }

    @Test
    void testClientFromKubeconfig() {
        String kubeconfigPath = getClass().getClassLoader().getResource("test.kubeconfig").getPath();
        KubeClient cl = new KubeClient(kubeconfigPath);
        assertEquals(kubeconfigPath, cl.getKubeconfigPath());
    }

}
