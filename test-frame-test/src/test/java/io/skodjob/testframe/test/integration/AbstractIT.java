package io.skodjob.testframe.test.integration;

import io.skodjob.testframe.annotations.ResourceManager;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.resources.NamespaceResource;
import io.skodjob.testframe.resources.ServiceAccountResource;
import io.skodjob.testframe.utils.KubeUtils;

@ResourceManager
public class AbstractIT {
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

    protected String nsName = "test";
}
