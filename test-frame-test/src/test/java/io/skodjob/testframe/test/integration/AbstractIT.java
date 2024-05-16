package io.skodjob.testframe.test.integration;

import io.skodjob.testframe.LoggerUtils;
import io.skodjob.testframe.annotations.ResourceManager;
import io.skodjob.testframe.annotations.TestVisualSeparator;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.utils.KubeUtils;

import java.util.concurrent.atomic.AtomicBoolean;

@ResourceManager
@TestVisualSeparator
public abstract class AbstractIT {
    static AtomicBoolean isCreateHandlerCalled = new AtomicBoolean(false);
    static AtomicBoolean isDeleteHandlerCalled = new AtomicBoolean(false);

    static {
        KubeResourceManager.getInstance().addCreateCallback(r -> {
            isCreateHandlerCalled.set(true);
            if (r.getKind().equals("Namespace")) {
                KubeUtils.labelNamespace(r.getMetadata().getName(), "test-label", "true");
            }
        });
        KubeResourceManager.getInstance().addDeleteCallback(r -> {
            isDeleteHandlerCalled.set(true);
            if (r.getKind().equals("Namespace")) {
                LoggerUtils.logResource("Deleted", r);
            }
        });
    }

    protected String nsName1 = "test";
    protected String nsName2 = "test2";
    protected String nsName3 = "test3";
    protected String nsName4 = "test4";
}
