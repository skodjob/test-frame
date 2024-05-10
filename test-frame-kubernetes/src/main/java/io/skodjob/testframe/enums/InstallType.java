/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.enums;

/**
 * Enum class containing names of installation methods for deployments (and operators):
 *      - Bundle (installation using YAML files)
 *      - Helm (installation using Helm charts)
 *      - OLM (installation using container catalog on the OperatorHub)
 */
public enum InstallType {
    Bundle,
    Helm,
    Olm,
    Unknown;

    /**
     * Returns enum based on String
     *
     * @param text  installation method in String
     *
     * @return one of the enums or {@link #Unknown} in case of not supported install type
     */
    public static InstallType fromString(String text) {
        for (InstallType installType : InstallType.values()) {
            if (installType.toString().equalsIgnoreCase(text)) {
                return installType;
            }
        }
        return Unknown;
    }
}