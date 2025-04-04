/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.test.integration.helpers;

import io.skodjob.testframe.interfaces.MustGatherSupplier;
import io.skodjob.testframe.LogCollector;
import io.skodjob.testframe.LogCollectorBuilder;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.test.integration.AbstractIT;
import org.junit.jupiter.api.extension.ExtensionContext;

public final class MustGatherSupplierImpl implements MustGatherSupplier {
    @Override
    public void saveKubernetesState(ExtensionContext context) {
        LogCollector logCollector = new LogCollectorBuilder()
            .withNamespacedResources(
                "configmap",
                "secret"
            )
            .withClusterWideResources("node")
            .withKubeClient(KubeResourceManager.get().kubeClient())
            .withKubeCmdClient(KubeResourceManager.get().kubeCmdClient())
            .withRootFolderPath(AbstractIT.LOG_DIR.resolve("failedTest")
                .resolve(context.getTestMethod().get().getName()).toString())
            .build();
        logCollector.collectFromNamespace("default");
        logCollector.collectClusterWideResources();
    }
}
