/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.clients;

import io.skodjob.testframe.environment.TestEnvironmentVariables;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class TestEnvironmentVariablesTest {

    @Test
    void testGetEnvVariablesCorrectly() {
        assertEquals(MyEnvs.MY_ENV, "this");
        assertEquals(MyEnvs.SECOND_ENV, "23");
        assertTrue(Files.exists(Paths.get(System.getProperty("user.dir"))
            .resolve("target").resolve("config.yaml")));
    }

    public static class MyEnvs {
        private static final Map<String, String> ENVS_MAP = Map.of(
            "MY_ENV", "this",
            "THIRD_ENV", "that"
        );

        public static final TestEnvironmentVariables ENVIRONMENT_VARIABLES = new TestEnvironmentVariables(ENVS_MAP);
        public static final String MY_ENV = ENVIRONMENT_VARIABLES.getOrDefault("MY_ENV", "");
        public static final String SECOND_ENV = ENVIRONMENT_VARIABLES.getOrDefault("SECOND_ENV", "23");

        static {
            ENVIRONMENT_VARIABLES.logEnvironmentVariables();
            try {
                ENVIRONMENT_VARIABLES.saveConfigurationFile(Paths.get(System.getProperty("user.dir"))
                    .resolve("target").toAbsolutePath().toString());
            } catch (IOException e) {
                fail("Env vars not saved");
            }
        }
    }
}
