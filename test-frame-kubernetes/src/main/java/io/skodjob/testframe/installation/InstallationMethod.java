/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.installation;

/**
 * Abstract class containing methods that can be implemented in particular setup classes based on the installation type.
 */
public abstract class InstallationMethod {
    /**
     * Deploy deployment/operator abstract method
     */
    public abstract void deploy();

    /**
     * Delete deployment/operator abstract method
     */
    public abstract void delete();
}
