package io.mcdxai.harness.util;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public final class MainThreadInvoker {
    private MainThreadInvoker() {
    }

    public static void run(Runnable action, Duration timeout) {
        call(() -> {
            action.run();
            return null;
        }, timeout);
    }

    public static <T> T call(ThrowingSupplier<T> supplier, Duration timeout) {
        if (mc == null) {
            throw new IllegalStateException("Minecraft client is not initialized.");
        }

        if (mc.isSameThread()) {
            try {
                return supplier.get();
            } catch (Exception e) {
                throw rethrow(e);
            }
        }

        CompletableFuture<T> future = new CompletableFuture<>();
        mc.execute(() -> {
            try {
                future.complete(supplier.get());
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw rethrow(e);
        }
    }

    private static RuntimeException rethrow(Exception e) {
        if (e instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new RuntimeException(e);
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}