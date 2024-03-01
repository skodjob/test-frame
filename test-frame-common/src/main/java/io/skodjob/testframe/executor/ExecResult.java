/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.executor;

import java.io.Serializable;

/**
 * Represents the result of an execution.
 */
public class ExecResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Return code of the execution
     */
    private final int returnCode;

    /**
     * standard output
     */
    private final String stdOut;

    /**
     * Error output
     */
    private final String stdErr;

    /**
     * Constructs a new ExecResult with the specified return code, standard output, and standard error.
     *
     * @param returnCode The return code.
     * @param stdOut     The standard output.
     * @param stdErr     The standard error.
     */
    ExecResult(int returnCode, String stdOut, String stdErr) {
        this.returnCode = returnCode;
        this.stdOut = stdOut;
        this.stdErr = stdErr;
    }

    /**
     * Checks if the execution was successful.
     *
     * @return {@code true} if the execution was successful, {@code false} otherwise.
     */
    public boolean exitStatus() {
        return returnCode == 0;
    }

    /**
     * Gets the return code of the execution.
     *
     * @return The return code.
     */
    public int returnCode() {
        return returnCode;
    }

    /**
     * Gets the standard output of the execution.
     *
     * @return The standard output.
     */
    public String out() {
        return stdOut;
    }

    /**
     * Gets the standard error of the execution.
     *
     * @return The standard error.
     */
    public String err() {
        return stdErr;
    }

    /**
     * Returns a string representation of the ExecResult.
     *
     * @return A string representation of the ExecResult.
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ExecResult{");
        sb.append("returnCode=").append(returnCode);
        sb.append(", stdOut='").append(stdOut).append('\'');
        sb.append(", stdErr='").append(stdErr).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
