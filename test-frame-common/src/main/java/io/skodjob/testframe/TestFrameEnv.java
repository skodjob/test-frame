/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

/**
 * Class which holds environment variables for system tests.
 */
public class TestFrameEnv {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestFrameEnv.class);
    private static final Map<String, String> VALUES = new HashMap<>();
    private static final Map<String, Object> YAML_DATA = loadConfigurationFile();

    private static final String CONFIG_FILE_PATH_ENV = "ENV_FILE";
    private static final String CLIENT_TYPE_ENV = "CLIENT_TYPE";
    private static final String TOKEN_ENV = "KUBE_TOKEN";
    private static final String URL_ENV = "KUBE_URL";

    /**
     * Default user dir of exec process
     */
    public static final String USER_PATH = System.getProperty("user.dir");
    /**
     * The type of client.
     */
    public static final String CLIENT_TYPE = getOrDefault(CLIENT_TYPE_ENV, TestFrameConstants.KUBERNETES_CLIENT);


    /**
     * The token for accessing the Kubernetes cluster.
     */
    public static final String KUBE_TOKEN = getOrDefault(TOKEN_ENV, null);

    /**
     * The URL for accessing the Kubernetes cluster.
     */
    public static final String KUBE_URL = getOrDefault(URL_ENV, null);

    private TestFrameEnv() {
        // Private constructor to prevent instantiation
    }

    static {
        String debugFormat = "{}: {}";
        LoggerUtils.logSeparator("-", 30);
        LOGGER.info("Used environment variables:");
        VALUES.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    if (!Objects.equals(entry.getValue(), "null")) {
                        LOGGER.info(debugFormat, entry.getKey(), entry.getValue());
                    }
                });
        LoggerUtils.logSeparator("-", 30);
    }

    /**
     * Print method.
     */
    public static void print() {
        // Method implementation
    }

    private static String getOrDefault(String varName, String defaultValue) {
        return getOrDefault(varName, String::toString, defaultValue);
    }

    private static <T> T getOrDefault(String var, Function<String, T> converter, T defaultValue) {
        String value = System.getenv(var) != null ?
                System.getenv(var) :
                (Objects.requireNonNull(YAML_DATA).get(var) != null ?
                        YAML_DATA.get(var).toString() :
                        null);
        T returnValue = defaultValue;
        if (value != null) {
            returnValue = converter.apply(value);
        }
        VALUES.put(var, String.valueOf(returnValue));
        return returnValue;
    }

    private static Map<String, Object> loadConfigurationFile() {
        String config = System.getenv().getOrDefault(CONFIG_FILE_PATH_ENV,
                Paths.get(System.getProperty("user.dir"), "config.yaml").toAbsolutePath().toString());
        Yaml yaml = new Yaml();
        try {
            File yamlFile = new File(config).getAbsoluteFile();
            return yaml.load(new FileInputStream(yamlFile));
        } catch (IOException ex) {
            LOGGER.info("Yaml configuration not provided or does not exist");
            return Collections.emptyMap();
        }
    }
}
