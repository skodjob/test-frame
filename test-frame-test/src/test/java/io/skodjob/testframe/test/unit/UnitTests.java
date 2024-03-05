/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.test.unit;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//TODO Implement mock kube to be able to work with RM
@EnableKubernetesMockClient(crud = true)
public class UnitTests {
    private KubernetesClient kubernetesClient;
    private KubernetesMockServer server;
    private static final Logger LOGGER = LoggerFactory.getLogger(UnitTests.class);

    @Test
    void tmpTest() {
        LOGGER.info("Placeholder for test");
    }
}
