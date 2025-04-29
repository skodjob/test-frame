/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.resources;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

public class KubeResourceManagerMockTest {
    @Test
    void testDeleteResourceWithWait() {
        AtomicBoolean deletionWaitWasCalled = new AtomicBoolean(false);
        KubeResourceManager kubeResourceManager = spy(KubeResourceManager.class);
        Namespace myNamespace = new NamespaceBuilder().withNewMetadata().withName("my-namespace").endMetadata().build();

        doAnswer(invocation -> {
            deletionWaitWasCalled.set(true);
            return true;
        }).when(kubeResourceManager).waitResourceCondition(any(), ArgumentMatchers.eq(ResourceCondition.deletion()));

        kubeResourceManager.deleteResourceWithWait(myNamespace);

        assertTrue(deletionWaitWasCalled.get());
    }

    @Test
    void testDeleteResourceWithWaitAsync() {
        KubeResourceManager kubeResourceManager = spy(KubeResourceManager.class);
        AtomicBoolean asyncWaitTriggered = new AtomicBoolean(false);
        Namespace myNamespace = new NamespaceBuilder().withNewMetadata().withName("my-namespace").endMetadata().build();

        doAnswer(invocation -> {
            boolean async = invocation.getArgument(1);

            if (async) {
                asyncWaitTriggered.set(true);
            }

            // Simulate the same logic if needed
            return null;
        }).when(kubeResourceManager).decideDeleteWaitAsync(anyList(), anyBoolean(), any());

        kubeResourceManager.deleteResourceWithAsyncWait(myNamespace);

        assertTrue(asyncWaitTriggered.get());
    }

    @Test
    void testDeleteResourceWithoutWait() {
        AtomicBoolean deletionWaitWasCalled = new AtomicBoolean(false);
        KubeResourceManager kubeResourceManager = spy(KubeResourceManager.class);
        Namespace myNamespace = new NamespaceBuilder().withNewMetadata().withName("my-namespace").endMetadata().build();

        doAnswer(invocation -> {
            deletionWaitWasCalled.set(true);
            return true;
        }).when(kubeResourceManager).waitResourceCondition(any(), eq(ResourceCondition.deletion()));

        kubeResourceManager.deleteResourceWithoutWait(myNamespace);

        assertFalse(deletionWaitWasCalled.get());
    }
}
