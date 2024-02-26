/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

public class LoggerUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggerUtils.class);
    static final String SEPARATOR_CHAR = "#";

    public static void logSeparator() {
        logSeparator(SEPARATOR_CHAR, 76);
    }

    public static void logSeparator(String delimiterChar, int length) {
        LOGGER.info(String.join("", Collections.nCopies(length, delimiterChar)));
    }
}
