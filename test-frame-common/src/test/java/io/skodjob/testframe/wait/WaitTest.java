package io.skodjob.testframe.wait;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaitTest {

    @Test
    public void testSuccessBeforeTimeout() {
        BooleanSupplier ready = () -> true; // Condition is immediately ready
        assertDoesNotThrow(() -> Wait.until("Test success", 100, 1000, ready));
    }

    @Test
    public void testTimeoutOccurs() {
        BooleanSupplier ready = () -> false; // Condition is never ready
        assertThrows(WaitException.class, () -> Wait.until("Test timeout", 100, 500, ready));
    }

    @Test
    public void testExceptionHandling() {
        BooleanSupplier ready = () -> { throw new RuntimeException("Failure"); };
        assertThrows(RuntimeException.class, () -> Wait.until("Test exception", 100, 1000, ready));
    }

    @Test
    public void testSyncNPEHandling() {
        BooleanSupplier ready = () -> { throw new NullPointerException("Null pointer exception"); };
        assertThrows(WaitException.class, () -> Wait.until("Test NPE", 100, 1000, ready));
    }

    @Test
    public void testAsyncSuccess() {
        BooleanSupplier ready = new BooleanSupplier() {
            private int count = 0;
            @Override
            public boolean getAsBoolean() {
                return ++count > 5; // Becomes true after 5 tries
            }
        };
        CompletableFuture<Void> future = Wait.untilAsync("Test async success", 100, 1000, ready);
        assertDoesNotThrow(() -> future.join());
    }

    @Test
    public void testAsyncTimeout() {
        BooleanSupplier ready = () -> false; // Condition is never ready
        CompletableFuture<Void> future = Wait.untilAsync("Test async timeout", 100, 500, ready);

        // Check for CompletionException due to the CompletableFuture's nature
        CompletionException thrown = assertThrows(CompletionException.class, future::join);
        assertTrue(thrown.getCause() instanceof TimeoutException, "Expected TimeoutException");
    }

    @Test
    public void testAsyncExceptionHandling() {
        BooleanSupplier ready = () -> { throw new RuntimeException("Failure in condition check"); };
        CompletableFuture<Void> future = Wait.untilAsync("Test async exception", 100, 1000, ready);

        // Expect CompletionException due to CompletableFuture's behavior
        CompletionException thrown = assertThrows(CompletionException.class, future::join);
        assertTrue(thrown.getCause() instanceof RuntimeException, "Expected RuntimeException");
    }

    @Test
    public void testAsyncImmediateTrueCondition() {
        BooleanSupplier ready = () -> true;
        CompletableFuture<Void> future = Wait.untilAsync("Test async immediate true", 100, 1000, ready);
        assertDoesNotThrow(() -> future.join());
    }

    @Test
    public void testAsyncNPEHandling() {
        BooleanSupplier ready = () -> { throw new NullPointerException("Null pointer exception"); };
        CompletableFuture<Void> future = Wait.untilAsync("Test async NPE", 100, 1000, ready);

        // Expect CompletionException due to CompletableFuture's behavior
        CompletionException thrown = assertThrows(CompletionException.class, future::join);
        assertTrue(thrown.getCause() instanceof NullPointerException, "Expected NullPointerException");
    }
}

