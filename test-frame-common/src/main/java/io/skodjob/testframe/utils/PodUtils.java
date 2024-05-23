/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.utils;

import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.readiness.Readiness;
import io.skodjob.testframe.TestFrameConstants;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.wait.Wait;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents utils class for pod
 */
public final class PodUtils {

    private static final Logger LOGGER = LogManager.getLogger(PodUtils.class);
    private static final long READINESS_TIMEOUT = Duration.ofMinutes(10).toMillis();

    private PodUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Wait for all pods in namespace to be ready
     *
     * @param namespaceName name of the namespace
     * @param containers flag wait for all containers
     * @param onTimeout callback on timeout
     */
    public static void waitForPodsReady(String namespaceName, boolean containers, Runnable onTimeout) {
        Wait.until("readiness of all Pods matching in namespace " + namespaceName,
                TestFrameConstants.GLOBAL_POLL_INTERVAL_MEDIUM, READINESS_TIMEOUT,
                () -> {
                    List<Pod> pods = KubeResourceManager.getKubeClient().getClient()
                            .pods().inNamespace(namespaceName).list().getItems();
                    if (pods.isEmpty()) {
                        LOGGER.debug("Expected Pods are not ready!");
                        return false;
                    }
                    for (Pod pod : pods) {
                        if (!(Readiness.isPodReady(pod) || Readiness.isPodSucceeded(pod))) {
                            LOGGER.debug("Pod not ready: {}/{}", namespaceName, pod.getMetadata().getName());
                            return false;
                        } else {
                            if (containers) {
                                for (ContainerStatus cs : pod.getStatus().getContainerStatuses()) {
                                    if (!(Boolean.TRUE.equals(cs.getReady())
                                            || cs.getState().getTerminated().getReason().equals("Completed"))) {
                                        LOGGER.debug("Container: {} of Pod: {}/{} not ready",
                                                namespaceName, pod.getMetadata().getName(), cs.getName());
                                        return false;
                                    }
                                }
                            }
                        }
                    }
                    LOGGER.info("Pods in namespace {} are ready", namespaceName);
                    return true;
                }, onTimeout);
    }

    /**
     * Wait for pods selected by label selector in namespace to be ready
     *
     * @param namespaceName namespace
     * @param selector label selector of the pods
     * @param expectPods expected pods count
     * @param containers flag wait for all containers
     * @param onTimeout callback on timeout
     */
    public static void waitForPodsReady(String namespaceName, LabelSelector selector, int expectPods,
                                        boolean containers, Runnable onTimeout) {
        Wait.until("readiness of all Pods matching: " + selector,
                TestFrameConstants.GLOBAL_POLL_INTERVAL_MEDIUM, READINESS_TIMEOUT,
                () -> {
                    List<Pod> pods = KubeResourceManager.getKubeClient().getClient().pods()
                            .inNamespace(namespaceName).withLabelSelector(selector).list().getItems();
                    if (pods.isEmpty() && expectPods == 0) {
                        LOGGER.debug("Expected Pods are ready");
                        return true;
                    }
                    if (pods.isEmpty()) {
                        LOGGER.debug("Pods matching: {}/{} are not ready", namespaceName, selector);
                        return false;
                    }
                    if (pods.size() != expectPods) {
                        LOGGER.debug("Expected Pods: {}/{} are not ready", namespaceName, selector);
                        return false;
                    }
                    for (Pod pod : pods) {
                        if (!(Readiness.isPodReady(pod) || Readiness.isPodSucceeded(pod))) {
                            LOGGER.debug("Pod not ready: {}/{}", namespaceName, pod.getMetadata().getName());
                            return false;
                        } else {
                            if (containers) {
                                for (ContainerStatus cs : pod.getStatus().getContainerStatuses()) {
                                    if (!(Boolean.TRUE.equals(cs.getReady())
                                            || cs.getState().getTerminated().getReason().equals("Completed"))) {
                                        LOGGER.debug("Container: {} of Pod: {}/{} not ready",
                                                namespaceName, pod.getMetadata().getName(), cs.getName());
                                        return false;
                                    }
                                }
                            }
                        }
                    }
                    LOGGER.info("Pods matching: {}/{} are ready", namespaceName, selector);
                    return true;
                }, onTimeout);
    }

    /**
     * Wait for pod ready, if not ready for timeout try to restart and check again
     *
     * @param namespace namespace
     * @param selector label selector
     * @param expectedPods expected pods count
     * @param containers flag wait for containers
     */
    public static void waitForPodsReadyWithRestart(String namespace, LabelSelector selector,
                                                   int expectedPods, boolean containers) {
        try {
            waitForPodsReady(namespace, selector, expectedPods, containers, () -> {
            });
        } catch (Exception ex) {
            LOGGER.warn("Pods not ready trying to restart");
            KubeResourceManager.getKubeClient().getClient().pods()
                    .inNamespace(namespace).withLabelSelector(selector).list().getItems().forEach(p ->
                            KubeResourceManager.getKubeClient().getClient().resource(p).delete());
            waitForPodsReady(namespace, selector, expectedPods, containers, () -> {
            });
        }
    }

    /**
     * Returns a map of resource name to resource version for all the pods in the given {@code namespace}
     * matching the given {@code selector}
     *
     * @param namespaceName namesapce
     * @param selector label selector
     * @return key value map podName -> uid
     */
    public static Map<String, String> podSnapshot(String namespaceName, LabelSelector selector) {
        List<Pod> pods = KubeResourceManager.getKubeClient().getClient().pods()
                .inNamespace(namespaceName).withLabelSelector(selector).list().getItems();
        return pods.stream()
                .collect(
                        Collectors.toMap(pod -> pod.getMetadata().getName(),
                                pod -> pod.getMetadata().getUid()));
    }

    /**
     * Verify if the pod is stable after it is in ready state.
     *
     * @param namespaceName namespace
     * @param labelSelector label selector
     */
    public static void verifyThatPodsAreStable(String namespaceName, LabelSelector labelSelector) {
        int[] stabilityCounter = {0};
        String phase = "Running";

        Wait.until(String.format("Pods in namespace '%s' with LabelSelector %s stability in phase %s",
                        namespaceName, labelSelector, phase),
                TestFrameConstants.GLOBAL_POLL_INTERVAL_SHORT, TestFrameConstants.GLOBAL_TIMEOUT,
                () -> {
                    List<Pod> existingPod = KubeResourceManager.getKubeClient().getClient().pods()
                            .inNamespace(namespaceName).withLabelSelector(labelSelector).list().getItems();
                    LOGGER.debug("Working with the following pods: {}", existingPod.stream()
                            .map(p -> p.getMetadata().getName()).toList());

                    for (Pod pod : existingPod) {
                        if (pod == null) {
                            continue;
                        }
                        if (pod.getStatus().getPhase().equals(phase)) {
                            LOGGER.debug("Pod: {}/{} is in the {} state. " +
                                            "Remaining seconds for Pod to be stable {}",
                                    namespaceName,
                                    pod.getMetadata().getName(),
                                    pod.getStatus().getPhase(),
                                    TestFrameConstants.GLOBAL_STABILITY_TIME -
                                            (TestFrameConstants.GLOBAL_POLL_INTERVAL_SHORT * stabilityCounter[0])
                            );
                        } else {
                            LOGGER.warn("Pod: {}/{} is not stable in phase following phase {} ({})" +
                                            " reset the stability counter from {}s to {}s",
                                    namespaceName, pod.getMetadata().getName(), pod.getStatus().getPhase(),
                                    phase, stabilityCounter[0], 0);
                            stabilityCounter[0] = 0;
                            return false;
                        }
                    }
                    stabilityCounter[0]++;

                    if (stabilityCounter[0] == TestFrameConstants.GLOBAL_STABILITY_TIME /
                            (TestFrameConstants.GLOBAL_POLL_INTERVAL_SHORT)) {
                        LOGGER.info("All Pods are stable {}", existingPod.stream()
                                .map(p -> p.getMetadata().getName()).collect(Collectors.joining(" ,")));
                        return true;
                    }
                    return false;
                });
    }
}
