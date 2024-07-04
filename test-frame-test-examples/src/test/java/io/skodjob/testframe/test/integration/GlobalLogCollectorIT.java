/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.test.integration;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.fail;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GlobalLogCollectorIT extends AbstractIT {
    @Test
    void testGlobalLogCollector() {
        fail("Expected issue");
    }

    @AfterEach
    void clean() throws IOException {
        FileUtils.deleteDirectory(LOG_DIR.toFile());
    }
}
