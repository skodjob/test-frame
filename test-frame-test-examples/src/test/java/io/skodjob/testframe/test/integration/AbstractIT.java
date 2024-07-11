/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.test.integration;

import io.skodjob.testframe.LogCollectorBuilder;
import io.skodjob.testframe.annotations.MustGather;
import io.skodjob.testframe.listeners.MustGatherController;
import io.skodjob.testframe.test.integration.helpers.GlobalLogCollectorTestHandler;
import io.skodjob.testframe.utils.LoggerUtils;
import io.skodjob.testframe.annotations.ResourceManager;
import io.skodjob.testframe.annotations.TestVisualSeparator;
import io.skodjob.testframe.resources.DeploymentType;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.resources.NamespaceType;
import io.skodjob.testframe.resources.ServiceAccountType;
import io.skodjob.testframe.utils.KubeUtils;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;

@ExtendWith(GlobalLogCollectorTestHandler.class) // For testing purpose
@ResourceManager
@MustGather
@TestVisualSeparator
public abstract class AbstractIT {
    static AtomicBoolean isCreateHandlerCalled = new AtomicBoolean(false);
    static AtomicBoolean isDeleteHandlerCalled = new AtomicBoolean(false);
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");
    public static final Path LOG_DIR = Paths.get(System.getProperty("user.dir"), "target", "logs")
        .resolve("test-run-" + DATE_FORMAT.format(LocalDateTime.now()));

    static {
        // Register resources which KRM uses for handling instead of native status check
        KubeResourceManager.getInstance().setResourceTypes(
            new NamespaceType(),
            new ServiceAccountType(),
            new DeploymentType()
        );

        // Register callback which are called with every create resource method for every resource
        KubeResourceManager.getInstance().addCreateCallback(r -> {
            isCreateHandlerCalled.set(true);
            if (r.getKind().equals("Namespace")) {
                KubeUtils.labelNamespace(r.getMetadata().getName(), "test-label", "true");
            }
        });

        // Register callback which are called with every delete resource method for every resource
        KubeResourceManager.getInstance().addDeleteCallback(r -> {
            isDeleteHandlerCalled.set(true);
            if (r.getKind().equals("Namespace")) {
                LoggerUtils.logResource("Deleted", r);
            }
        });

        // Setup global log collector and handlers
        MustGatherController.setupGlobalLogCollector(new LogCollectorBuilder()
            .withNamespacedResources("sa", "deployment", "configmaps", "secret")
            .withClusterWideResources("nodes")
            .withKubeClient(KubeResourceManager.getKubeClient())
            .withKubeCmdClient(KubeResourceManager.getKubeCmdClient())
            .withRootFolderPath(LOG_DIR.toString())
            .build());
        MustGatherController.setLogCallback(() -> {
            MustGatherController.getGlobalLogCollector().collectFromNamespaces("default");
            MustGatherController.getGlobalLogCollector().collectClusterWideResources();
        });
    }

    protected String nsName1 = "test";
    protected String nsName2 = "test2";
    protected String nsName3 = "test3";
    protected String nsName4 = "test4";
}
