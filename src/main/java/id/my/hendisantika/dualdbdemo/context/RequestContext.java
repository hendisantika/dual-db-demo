package id.my.hendisantika.dualdbdemo.context;

/**
 * Request context holder using Scoped Values (JDK 25 Preview Feature).
 * Scoped Values provide a safe and efficient way to share immutable data
 * across threads, especially with Virtual Threads and Structured Concurrency.
 * <p>
 * Benefits over ThreadLocal:
 * - Immutable by design (safer for concurrent access)
 * - Automatically inherited by child threads in StructuredTaskScope
 * - More efficient memory usage with Virtual Threads
 * - Clear lifecycle boundaries with try-with-resources pattern
 */
public final class RequestContext {

    /**
     * Scoped value for tracking request/correlation ID across concurrent operations.
     * This ID is automatically propagated to forked subtasks in StructuredTaskScope.
     */
    public static final ScopedValue<String> CORRELATION_ID = ScopedValue.newInstance();

    /**
     * Scoped value for tracking the current user context.
     */
    public static final ScopedValue<String> USER_ID = ScopedValue.newInstance();

    /**
     * Scoped value for tracking the operation being performed.
     */
    public static final ScopedValue<String> OPERATION = ScopedValue.newInstance();

    private RequestContext() {
        // Utility class - prevent instantiation
    }

    /**
     * Get the current correlation ID, or "unknown" if not set.
     */
    public static String getCorrelationId() {
        return CORRELATION_ID.orElse("unknown");
    }

    /**
     * Get the current user ID, or "anonymous" if not set.
     */
    public static String getUserId() {
        return USER_ID.orElse("anonymous");
    }

    /**
     * Get the current operation name, or "unknown" if not set.
     */
    public static String getOperation() {
        return OPERATION.orElse("unknown");
    }
}
