/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.environment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.skodjob.testframe.utils.LoggerUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Class representing environment variables used in the test suite
 */
public class TestEnvironmentVariables {

    private static final Logger LOGGER = LogManager.getLogger(TestEnvironmentVariables.class);

    /* test */ private final Map<String, String> envMap;
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
     * @param envMap Map containing the environment variables. Used mainly for testing purposes.
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
     * @param envVarName   environment variable name
     * @param defaultValue default value, which should be used if the env var is empty and the config file
     *                     doesn't contain it
     * @return value from env var/config file or default
     */
    public String getOrDefault(String envVarName, String defaultValue) {
        return getOrDefault(envVarName, String::toString, defaultValue);
    }

    /**
     * Method which returns the value from env variable or its default in the specified type.
     * It also checks if the env var is specified in the configuration file before applying the default.
     *
     * @param envVarName   environment variable name
     * @param converter    converter to the desired type
     * @param defaultValue default value, which should be used if the env var is empty and the config file
     *                     doesn't contain it
     * @param <T>          desired type
     * @return value from env var/config file or default
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
     * @return Map with env variables and their values, or empty Map in case of not existing file
     */
    protected Map<String, Object> loadConfigurationFile() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            File yamlFile = new File(configFilePath).getAbsoluteFile();
            return mapper.readValue(new File(yamlFile.getAbsoluteFile().toString()),
                new TypeReference<HashMap<String, Object>>() {
                });
        } catch (IOException ex) {
            LOGGER.info("Yaml configuration not provider or not exists");
            return Collections.emptyMap();
        }
    }

    /***
     * Saves all set environment variables into yaml file
     *
     * @param testLogDir dir where to store file with set env vars
     * @throws IOException ioException
     */
    public void saveConfigurationFile(String testLogDir) throws IOException {
        Path logPath = Path.of(testLogDir);
        Files.createDirectories(logPath);

        LinkedHashMap<String, String> toSave = new LinkedHashMap<>(values);

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.writerWithDefaultPrettyPrinter().writeValue(logPath.resolve("config.yaml").toFile(), toSave);
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
