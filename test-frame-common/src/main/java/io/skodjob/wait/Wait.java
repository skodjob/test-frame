package io.skodjob.wait;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.util.function.BooleanSupplier;

public class Wait {

    /**
     * For every poll (happening once each {@code pollIntervalMs}) checks if supplier {@code ready} is true.
     * If yes, the wait is closed. Otherwise, waits another {@code pollIntervalMs} and tries again.
     * Once the wait timeout (specified by {@code timeoutMs} is reached and supplier wasn't true until that time,
     * throws {@link WaitException}.
     *
     * @param description information about on what we are waiting
     * @param pollIntervalMs poll interval in milliseconds
     * @param timeoutMs timeout specified in milliseconds
     * @param ready {@link BooleanSupplier} containing code, which should be executed each poll, verifying readiness
     *                                     of the particular thing
     */
    public static void until(String description, long pollIntervalMs, long timeoutMs, BooleanSupplier ready) {
        until(description, pollIntervalMs, timeoutMs, ready, () -> {});
    }

    /**
     * For every poll (happening once each {@code pollIntervalMs}) checks if supplier {@code ready} is true.
     * If yes, the wait is closed. Otherwise, waits another {@code pollIntervalMs} and tries again.
     * Once the wait timeout (specified by {@code timeoutMs} is reached and supplier wasn't true until that time,
     * runs the {@code onTimeout} (f.e. print of logs, showing the actual value that was checked inside {@code ready}),
     * and finally throws {@link WaitException}.
     *
     * @param description information about on what we are waiting
     * @param pollIntervalMs poll interval in milliseconds
     * @param timeoutMs timeout specified in milliseconds
     * @param ready {@link BooleanSupplier} containing code, which should be executed each poll, verifying readiness
     *                                     of the particular thing
     * @param onTimeout {@link Runnable} executed once timeout is reached and before the {@link WaitException} is thrown.
     */
    public static void until(String description, long pollIntervalMs, long timeoutMs, BooleanSupplier ready, Runnable onTimeout) {
        System.out.println("Waiting for " + description);
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
                    System.out.println("While waiting for " + description + " exception occurred: " + exceptionMessage);
                    // log the stacktrace
                    e.printStackTrace(new PrintWriter(stackTraceError));
                } else if (exceptionMessage != null && !exceptionMessage.equals(previousExceptionMessage) && ++newExceptionAppearance == 2) {
                    previousExceptionMessage = exceptionMessage;
                }

                result = false;
            }
            long timeLeft = deadline - System.currentTimeMillis();
            if (result) {
                return;
            }
            if (timeLeft <= 0) {
                if (exceptionCount > 1) {
                    System.out.println("Exception waiting for " + description + ", " + exceptionMessage);

                    if (!stackTraceError.toString().isEmpty()) {
                        // printing handled stacktrace
                        System.out.println(stackTraceError);
                    }
                }
                onTimeout.run();
                WaitException waitException = new WaitException("Timeout after " + timeoutMs + " ms waiting for " + description);
                waitException.printStackTrace();
                throw waitException;
            }
            long sleepTime = Math.min(pollIntervalMs, timeLeft);
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                return;
            }
        }
    }
}
