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

    /**
     * Poll the given {@code ready} function every {@code pollIntervalMs} milliseconds until it returns true,
     * or throw a WaitException if it doesn't return true within {@code timeoutMs} milliseconds.
     *
     * @return The remaining time left until timeout occurs
     * (helpful if you have several calls which need to share a common timeout),
     */
    public static long waitFor(String description, long pollIntervalMs, long timeoutMs, BooleanSupplier ready) {
        return waitFor(description, pollIntervalMs, timeoutMs, ready, () -> { });
    }

    public static long waitFor(String description, long pollIntervalMs, long timeoutMs, BooleanSupplier ready, Runnable onTimeout) {
        LOGGER.debug("Waiting for {}", description);
        long deadline = System.currentTimeMillis() + timeoutMs;

        String exceptionMessage = null;
        String previousExceptionMessage = null;

        // in case we are polling every 1s, we want to print exception after x tries, not on the first try
        // for minutes poll interval will 2 be enough
        int exceptionAppearanceCount = Duration.ofMillis(pollIntervalMs).toMinutes() > 0 ? 2 : Math.max((int) (timeoutMs / pollIntervalMs) / 4, 2);
        int exceptionCount = 0;
        int newExceptionAppearance = 0;

        StringWriter stackTraceError = new StringWriter();

        while (true) {
            boolean result;
            try {
                result = ready.getAsBoolean();
            } catch (Exception e) {
                exceptionMessage = e.getMessage();

                if (++exceptionCount == exceptionAppearanceCount && exceptionMessage != null && exceptionMessage.equals(previousExceptionMessage)) {
                    LOGGER.error("While waiting for {} exception occurred: {}", description, exceptionMessage);
                    // log the stacktrace
                    e.printStackTrace(new PrintWriter(stackTraceError));
                } else if (exceptionMessage != null && !exceptionMessage.equals(previousExceptionMessage) && ++newExceptionAppearance == 2) {
                    previousExceptionMessage = exceptionMessage;
                }

                result = false;
            }
            long timeLeft = deadline - System.currentTimeMillis();
            if (result) {
                return timeLeft;
            }
            if (timeLeft <= 0) {
                if (exceptionCount > 1) {
                    LOGGER.error("Exception waiting for {}, {}", description, exceptionMessage);

                    if (!stackTraceError.toString().isEmpty()) {
                        // printing handled stacktrace
                        LOGGER.error(stackTraceError.toString());
                    }
                }
                onTimeout.run();
                WaitException waitException = new WaitException("Timeout after " + timeoutMs + " ms waiting for " + description);
                waitException.printStackTrace();
                throw waitException;
            }
            long sleepTime = Math.min(pollIntervalMs, timeLeft);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("{} not ready, will try again in {} ms ({}ms till timeout)", description, sleepTime, timeLeft);
            }
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                return deadline - System.currentTimeMillis();
            }
        }
    }

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(new ThreadFactory() {
        final ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();

        @Override
        public Thread newThread(Runnable r) {
            Thread result = defaultThreadFactory.newThread(r);
            result.setDaemon(true);
            return result;
        }
    });

    public static CompletableFuture<Void> asyncWaitFor(String description, long pollIntervalMs, long timeoutMs, BooleanSupplier ready) {
        LOGGER.info("Waiting for {}", description);
        long deadline = System.currentTimeMillis() + timeoutMs;
        CompletableFuture<Void> future = new CompletableFuture<>();
        Executor delayed = CompletableFuture.delayedExecutor(pollIntervalMs, TimeUnit.MILLISECONDS, EXECUTOR);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                boolean result;
                try {
                    result = ready.getAsBoolean();
                } catch (Exception e) {
                    future.completeExceptionally(e);
                    return;
                }
                long timeLeft = deadline - System.currentTimeMillis();
                if (!future.isDone()) {
                    if (!result) {
                        if (timeLeft >= 0) {
                            if (LOGGER.isTraceEnabled()) {
                                LOGGER.trace("{} not ready, will try again ({}ms till timeout)", description, timeLeft);
                            }
                            delayed.execute(this);
                        } else {
                            future.completeExceptionally(new TimeoutException(String.format("Waiting for %s timeout %s exceeded", description, timeoutMs)));
                        }
                    } else {
                        future.complete(null);
                    }
                }
            }
        };
        r.run();
        return future;
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
