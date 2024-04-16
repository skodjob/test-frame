/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.utils;

import io.fabric8.openshift.api.model.operatorhub.v1alpha1.InstallPlan;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.InstallPlanBuilder;
import io.skodjob.testframe.TestFrameConstants;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.wait.Wait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for Kubernetes and Openshift clusters.
 */
public class KubeUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(KubeUtils.class);

    private KubeUtils() {
    }

    /**
     * Approve install plan in namespace
     *
     * @param namespaceName   namespace
     * @param installPlanName install plan name
     */
    public static void approveInstallPlan(String namespaceName, String installPlanName) {
        LOGGER.debug("Approving InstallPlan {}", installPlanName);
        Wait.until("InstallPlan approval", TestFrameConstants.GLOBAL_POLL_INTERVAL_SHORT, 15_000, () -> {
            try {
                InstallPlan installPlan =
                        new InstallPlanBuilder(KubeResourceManager.getKubeClient()
                                .getOpenShiftClient().operatorHub().installPlans()
                                .inNamespace(namespaceName).withName(installPlanName).get())
                                .editSpec()
                                .withApproved()
                                .endSpec()
                                .build();

                KubeResourceManager.getKubeClient().getOpenShiftClient().operatorHub().installPlans()
                        .inNamespace(namespaceName).withName(installPlanName).patch(installPlan);
                return true;
            } catch (Exception ex) {
                LOGGER.error(String.valueOf(ex));
                return false;
            }
        });
    }

    /**
     * Read not approved install-plans with prefix
     *
     * @param namespaceName namespace
     * @param csvPrefix     prefix of install plans
     * @return list of not approved install-plans
     */
    public InstallPlan getNonApprovedInstallPlan(String namespaceName, String csvPrefix) {
        return KubeResourceManager.getKubeClient().getOpenShiftClient().operatorHub().installPlans()
                .inNamespace(namespaceName).list().getItems().stream()
                .filter(installPlan -> !installPlan.getSpec().getApproved()
                        && installPlan.getSpec().getClusterServiceVersionNames().toString().contains(csvPrefix))
                .findFirst().get();
    }
}
