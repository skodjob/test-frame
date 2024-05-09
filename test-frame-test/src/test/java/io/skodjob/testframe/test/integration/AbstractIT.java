package io.skodjob.testframe.test.integration;

import io.skodjob.testframe.annotations.ResourceManager;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.resources.NamespaceResource;
import io.skodjob.testframe.resources.ServiceAccountResource;
import io.skodjob.testframe.utils.KubeUtils;

@ResourceManager
public abstract class AbstractIT {
    static {
        KubeResourceManager.getInstance().setResourceTypes(
                new NamespaceResource(),
                new ServiceAccountResource()
        );
        KubeResourceManager.getInstance().addCreateCallback(r -> {
            if (r.getKind().equals("Namespace")) {
                KubeUtils.labelNamespace(r.getMetadata().getName(), "test-label", "true");
            }
        });
    }

    protected String nsName1 = "test";
    protected String nsName2 = "test2";
    protected String nsName3 = "test3";
    protected String nsName4 = "test4";
}
