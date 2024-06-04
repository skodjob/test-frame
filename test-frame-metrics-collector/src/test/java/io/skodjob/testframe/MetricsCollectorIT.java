package io.skodjob.testframe;

import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.skodjob.testframe.clients.cmdClient.KubeCmdClient;
import io.skodjob.testframe.clients.cmdClient.Kubectl;
import io.skodjob.testframe.exceptions.IncompleteMetricsException;
import io.skodjob.testframe.exceptions.MetricsCollectionException;
import io.skodjob.testframe.exceptions.NoPodsFoundException;
import io.skodjob.testframe.executor.Exec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class MetricsCollectorIT {

    private KubernetesClient mockClient;
    private KubeCmdClient mockCmdClient;

    private MetricsCollector metricsCollector;

    @BeforeEach
    public void setup() {
        mockClient = mock(KubernetesClient.class);
        mockCmdClient = mock(KubeCmdClient.class);

        // we need to use "kubectl" instead of toString because `"Mock for KubeCmdClient, hashCode: 1774795940"`
        when(mockCmdClient.inNamespace(anyString())).thenReturn(new Kubectl());

        MetricsCollector.Builder builder = new MetricsCollector.Builder()
            .withNamespaceName("test-namespace")
            .withScraperPodName("scraper-pod")
            .withComponent(new MetricsComponent() {
                public int getDefaultMetricsPort() { return 8080; }
                public String getDefaultMetricsPath() { return "/metrics"; }
                public LabelSelector getLabelSelector() { return new LabelSelector(); }
            });

        this.metricsCollector = builder.build();

        // Dependency Injection of external components
        this.metricsCollector.setKubeClient(mockClient);
        this.metricsCollector.setKubeCmdClient(mockCmdClient);
    }

    @Test
    public void testCollectMetricsFromPods() throws InterruptedException, ExecutionException, IOException, MetricsCollectionException {
        // Create mock operations for Kubernetes client
        MixedOperation<Pod, PodList, PodResource> podsOperation = mock(MixedOperation.class);
        when(mockClient.pods()).thenReturn(podsOperation);

        // Mock the inNamespace and withLabelSelector behavior
        MixedOperation<Pod, PodList, PodResource> namespaceOperation = mock(MixedOperation.class);
        when(podsOperation.inNamespace(anyString())).thenReturn(namespaceOperation);
        when(namespaceOperation.withLabelSelector((LabelSelector) any())).thenReturn(namespaceOperation);

        Pod pod = new PodBuilder()
            .withNewMetadata()
                .withName("pod1")
                .withNamespace("test-namespace")
            .endMetadata()
            .withNewStatus()
                .withPodIP("192.168.1.1")
            .endStatus()
            .build();

        PodList podList = new PodList();
        podList.setItems(List.of(pod));

        when(namespaceOperation.list()).thenReturn(podList);

        Exec mockExec = mock(Exec.class);
        // Ensure all arguments use matchers if any argument uses a matcher.
        when(mockExec.execute(eq(null), anyList(), eq(null), eq(20000L))).thenReturn(0);
        when(mockExec.out()).thenReturn("metric1 100\nmetric2 200");

        this.metricsCollector.setExec(mockExec);
        this.metricsCollector.collectMetricsFromPods(5000);

        // Verify interactions and assertions
        verify(mockExec, times(1)).execute(eq(null), anyList(), eq(null), eq(20000L));
        assertNotNull(this.metricsCollector.getCollectedData());
    }

    @Test
    public void testCollectMetricsWhenNoPodsAvailable() throws Exception {
        // Create mock operations for Kubernetes client
        MixedOperation<Pod, PodList, PodResource> podsOperation = mock(MixedOperation.class);
        when(mockClient.pods()).thenReturn(podsOperation);

        // Mock the inNamespace and withLabelSelector behavior
        MixedOperation<Pod, PodList, PodResource> namespaceOperation = mock(MixedOperation.class);
        when(podsOperation.inNamespace(anyString())).thenReturn(namespaceOperation);
        when(namespaceOperation.withLabelSelector((LabelSelector) any())).thenReturn(namespaceOperation);

        // Mock Kubernetes client operations
        when(namespaceOperation.list())
            .thenReturn(new PodList());  // Empty pod list

        // This would throw WaitException because we do not have any data scraped
        assertThrows(NoPodsFoundException.class, () -> this.metricsCollector.collectMetricsFromPods(2000));

        // Verify interactions
        verify(mock(Exec.class), times(0)).execute(eq(null), anyList(), eq(null), eq(20000L));
        assertTrue(this.metricsCollector.getCollectedData().isEmpty());
    }

    @Test
    public void testCollectMetricsWithIncompleteData() throws IOException, ExecutionException, InterruptedException {
        // Create mock operations for Kubernetes client
        MixedOperation<Pod, PodList, PodResource> podsOperation = mock(MixedOperation.class);
        when(mockClient.pods()).thenReturn(podsOperation);

        // Mock the inNamespace and withLabelSelector behavior
        MixedOperation<Pod, PodList, PodResource> namespaceOperation = mock(MixedOperation.class);
        when(podsOperation.inNamespace(anyString())).thenReturn(namespaceOperation);
        when(namespaceOperation.withLabelSelector((LabelSelector) any())).thenReturn(namespaceOperation);

        // Create a pod with valid metadata but empty metrics data
        Pod pod = new PodBuilder()
                    .withNewMetadata()
                        .withName("pod1")
                        .withNamespace("test-namespace")
                    .endMetadata()
                    .withNewStatus()
                        .withPodIP("192.168.1.100")
                    .endStatus()
                    .build();
        PodList podList = new PodList();
        podList.setItems(List.of(pod));
        when(namespaceOperation.list()).thenReturn(podList);

        Exec mockExec = mock(Exec.class);
        // Mock exec behavior to return empty metrics data
        when(mockExec.execute(any(), any(), any(), anyLong())).thenReturn(0);
        when(mockExec.out()).thenReturn("");  // Empty metrics output

        // Set the mock exec in metrics collector
        metricsCollector.setExec(mockExec);

        // Execute and expect an IncompleteMetricsException
        assertThrows(IncompleteMetricsException.class, () -> metricsCollector.collectMetricsFromPods(2000));

        // Verify interactions
        // at least once interactions, because we use waitUntil with 1s
        verify(mockExec, atLeastOnce()).execute(any(), any(), any(), anyLong());
        assertTrue(metricsCollector.getCollectedData().isEmpty());
    }

    @Test
    public void testCollectMetricsWithExecutionError() throws IOException, ExecutionException, InterruptedException {
        // Create mock operations for Kubernetes client
        MixedOperation<Pod, PodList, PodResource> podsOperation = mock(MixedOperation.class);
        when(mockClient.pods()).thenReturn(podsOperation);

        // Mock the inNamespace and withLabelSelector behavior
        MixedOperation<Pod, PodList, PodResource> namespaceOperation = mock(MixedOperation.class);
        when(podsOperation.inNamespace(anyString())).thenReturn(namespaceOperation);
        when(namespaceOperation.withLabelSelector((LabelSelector) any())).thenReturn(namespaceOperation);

        // Create a pod with valid metadata
        Pod pod = new PodBuilder()
                    .withNewMetadata()
                        .withName("pod2")
                        .withNamespace("test-namespace")
                    .endMetadata()
                    .withNewStatus()
                        .withPodIP("192.168.1.101")
                    .endStatus()
                    .build();
        PodList podList = new PodList();
        podList.setItems(List.of(pod));
        when(namespaceOperation.list()).thenReturn(podList);

        Exec mockExec = mock(Exec.class);

        // Simulate an execution error
        when(mockExec.execute(any(), any(), any(), anyLong())).thenThrow(new IOException("Failed to execute command"));

        // Set the mock exec in metrics collector
        metricsCollector.setExec(mockExec);

        // Execute and expect a MetricsCollectionException
        assertThrows(MetricsCollectionException.class, () -> metricsCollector.collectMetricsFromPods(1000));

        // Verify interactions
        verify(mockExec, atLeastOnce()).execute(any(), any(), any(), anyLong());
        assertTrue(metricsCollector.getCollectedData().isEmpty());
    }
}
