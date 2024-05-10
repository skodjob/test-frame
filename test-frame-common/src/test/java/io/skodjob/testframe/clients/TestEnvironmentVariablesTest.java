package io.skodjob.testframe.clients;

import io.skodjob.testframe.environment.TestEnvironmentVariables;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestEnvironmentVariablesTest {

    @Test
    void testGetEnvVariablesCorrectly() {
        assertEquals(MyEnvs.MY_ENV, "this");
        assertEquals(MyEnvs.SECOND_ENV, "23");
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
        }
    }
}
