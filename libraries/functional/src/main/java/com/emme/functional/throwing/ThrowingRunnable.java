package com.emme.functional.throwing;

/**
 * A runnable parameterized by the exception type it may throw.
 * <pre>{@code
 * ThrowingRunnable<IOException> task = () -> { throw new IOException("boom"); };
 * }</pre>
 */
@FunctionalInterface
public interface ThrowingRunnable<E extends Throwable> {
    void run() throws E;
}
