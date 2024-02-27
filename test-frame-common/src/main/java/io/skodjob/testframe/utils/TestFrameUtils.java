/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.skodjob.testframe.wait.WaitException;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;

@SuppressWarnings({"checkstyle:ClassFanOutComplexity"})
public final class TestFrameUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestFrameUtils.class);

    /**
     * Default timeout for asynchronous tests.
     */
    public static final int DEFAULT_TIMEOUT_DURATION = 30;

    /**
     * Default timeout unit for asynchronous tests.
     */
    public static final TimeUnit DEFAULT_TIMEOUT_UNIT = TimeUnit.SECONDS;

    private TestFrameUtils() {
        // All static methods
    }

    public static InputStream getFileFromResourceAsStream(String fileName) {

        // The class loader that loaded the class
        ClassLoader classLoader = TestFrameUtils.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileName);

        // the stream holding the file content
        if (inputStream == null) {
            throw new IllegalArgumentException("file not found! " + fileName);
        } else {
            return inputStream;
        }

    }

    public static <T> T configFromYaml(String yamlFile, Class<T> c) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            return mapper.readValue(yamlFile, c);
        } catch (InvalidFormatException e) {
            throw new IllegalArgumentException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Repeat command n-times
     *
     * @param retry count of remaining retries
     * @param fn request function
     * @return The value from the first successful call to the callable
     */
    public static <T> T runUntilPass(int retry, Callable<T> fn) {
        for (int i = 0; i < retry; i++) {
            try {
                LOGGER.debug("Running command, attempt: {}", i);
                return fn.call();
            } catch (Exception | Error ex) {
                LOGGER.warn("Command failed: {}", ex.getMessage());
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        throw new IllegalStateException(String.format("Command wasn't pass in %s attempts", retry));
    }
}
