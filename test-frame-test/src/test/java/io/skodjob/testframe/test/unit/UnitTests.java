/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.test.unit;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import org.junit.jupiter.api.Test;

//TODO Implement mock kube to be able to work with RM
@EnableKubernetesMockClient(crud = true)
public class UnitTests {
    private KubernetesClient kubernetesClient;
    private KubernetesMockServer server;

    @Test
    void tmpTest() {
        System.out.println("Placeholder for test");
    }
}
