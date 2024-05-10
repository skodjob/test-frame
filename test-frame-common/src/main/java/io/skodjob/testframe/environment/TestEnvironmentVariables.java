/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.environment;

import io.skodjob.testframe.LoggerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Class representing environment variables used in the test suite
 */
public class TestEnvironmentVariables {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestEnvironmentVariables.class);

    /* test */ private Map<String, String> envMap;
    private Map<String, String> values = new HashMap<>();
    private Map<String, Object> yamlData = new HashMap<>();
    private final String configFilePath;
    private final String configFilePathEnv = "ENV_FILE";

    /**
     * {@link TestEnvironmentVariables} object initialization, where the config file is loaded to {@link #yamlData}
     * if possible.
     */
    public TestEnvironmentVariables() {
        this(System.getenv());
    }

    /**
     * {@link TestEnvironmentVariables} object initialization, where the config file is loaded to {@link #yamlData}
     * if possible.
     *
     * @param envMap    Map containing the environment variables. Used mainly for testing purposes.
     */
    public TestEnvironmentVariables(Map<String, String> envMap) {
        this.envMap = envMap;
        this.configFilePath = envMap.getOrDefault(configFilePathEnv,
            Paths.get(System.getProperty("user.dir"), "config.yaml").toAbsolutePath().toString());
        this.yamlData = loadConfigurationFile();
    }

    /**
     * Method which returns the value from env variable or its default in String.
     *
     * @param envVarName        environment variable name
     * @param defaultValue      default value, which should be used if the env var is empty and the config file
     *                          doesn't contain it
     *
     * @return  value from env var/config file or default
     */
    public String getOrDefault(String envVarName, String defaultValue) {
        return getOrDefault(envVarName, String::toString, defaultValue);
    }

    /**
     * Method which returns the value from env variable or its default in the specified type.
     * It also checks if the env var is specified in the configuration file before applying the default.
     *
     * @param envVarName        environment variable name
     * @param converter         converter to the desired type
     * @param defaultValue      default value, which should be used if the env var is empty and the config file
     *                          doesn't contain it
     *
     * @return  value from env var/config file or default
     * @param <T>   desired type
     */
    public <T> T getOrDefault(String envVarName, Function<String, T> converter, T defaultValue) {
        String value = envMap.get(envVarName) != null ?
            envMap.get(envVarName) :
            (Objects.requireNonNull(yamlData).get(envVarName) != null ?
                yamlData.get(envVarName).toString() :
                null);

        T returnValue = defaultValue;

        if (value != null) {
            returnValue = converter.apply(value);
        }

        values.put(envVarName, String.valueOf(returnValue));
        return returnValue;
    }

    /**
     * Method that loads configuration file - either from specified path by {@link #configFilePathEnv} or from
     * the default path (in the `config.yaml` file on the `user.dir` path).
     * If the file doesn't exist, the info log is printed and empty Map is returned.
     *
     * @return  Map with env variables and their values, or empty Map in case of not existing file
     */
    protected Map<String, Object> loadConfigurationFile() {
        Yaml yaml = new Yaml();

        try {
            File yamlFile = new File(configFilePath).getAbsoluteFile();
            return yaml.load(new FileInputStream(yamlFile));
        } catch (IOException ex) {
            LOGGER.info("Yaml configuration not provided or does not exist");
            return Collections.emptyMap();
        }
    }

    /**
     * Method which logs environment variables parsed by {@link TestEnvironmentVariables}
     */
    public void logEnvironmentVariables() {
        String debugFormat = "{}: {}";

        LoggerUtils.logSeparator("-", 30);

        LOGGER.info("Used environment variables:");

        values.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                if (!Objects.equals(entry.getValue(), "null")) {
                    LOGGER.info(debugFormat, entry.getKey(), entry.getValue());
                }
            });

        LoggerUtils.logSeparator("-", 30);
    }
}
