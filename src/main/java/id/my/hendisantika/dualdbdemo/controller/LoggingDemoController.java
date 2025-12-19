package id.my.hendisantika.dualdbdemo.controller;

import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller demonstrating Log4j2 logging capabilities.
 * This showcases different log levels, markers, and structured logging.
 */
@Log4j2
@RestController
@RequestMapping("/api/logging")
public class LoggingDemoController {

    private static final Marker PERFORMANCE_MARKER = MarkerManager.getMarker("PERFORMANCE");
    private static final Marker SECURITY_MARKER = MarkerManager.getMarker("SECURITY");
    private static final Marker BUSINESS_MARKER = MarkerManager.getMarker("BUSINESS");

    /**
     * Demonstrates all log levels available in Log4j2
     */
    @GetMapping("/levels")
    public ResponseEntity<Map<String, String>> demonstrateLogLevels() {
        log.trace("This is a TRACE level message - most detailed logging");
        log.debug("This is a DEBUG level message - debugging information");
        log.info("This is an INFO level message - general information");
        log.warn("This is a WARN level message - potential issues");
        log.error("This is an ERROR level message - error conditions");
        log.fatal("This is a FATAL level message - critical failures");

        return ResponseEntity.ok(Map.of(
                "message", "All log levels demonstrated - check console and log files",
                "levels", "TRACE, DEBUG, INFO, WARN, ERROR, FATAL"
        ));
    }

    /**
     * Demonstrates parameterized logging (more efficient than string concatenation)
     */
    @GetMapping("/parameterized/{name}/{count}")
    public ResponseEntity<Map<String, Object>> demonstrateParameterizedLogging(
            @PathVariable String name,
            @PathVariable int count) {

        // Efficient parameterized logging - no string concatenation if log level is disabled
        log.info("Processing request for user: {} with count: {}", name, count);
        log.debug("Additional details - name length: {}, count doubled: {}", name.length(), count * 2);

        // Supplier-based lazy evaluation for expensive operations
        log.debug("Expensive calculation result: {}", () -> performExpensiveCalculation(count));

        return ResponseEntity.ok(Map.of(
                "name", name,
                "count", count,
                "message", "Parameterized logging demonstrated"
        ));
    }

    /**
     * Demonstrates marker-based logging for categorization
     */
    @GetMapping("/markers/{action}")
    public ResponseEntity<Map<String, String>> demonstrateMarkers(@PathVariable String action) {
        // Performance marker for timing-related logs
        long startTime = System.nanoTime();
        log.info(PERFORMANCE_MARKER, "Starting action: {}", action);

        // Simulate some work
        simulateWork();

        long duration = System.nanoTime() - startTime;
        log.info(PERFORMANCE_MARKER, "Action '{}' completed in {} ms", action, duration / 1_000_000);

        // Security marker for security-related logs
        log.info(SECURITY_MARKER, "User performed action: {}", action);

        // Business marker for business logic logs
        log.info(BUSINESS_MARKER, "Business operation completed: {}", action);

        return ResponseEntity.ok(Map.of(
                "action", action,
                "message", "Marker-based logging demonstrated",
                "markers", "PERFORMANCE, SECURITY, BUSINESS"
        ));
    }

    /**
     * Demonstrates exception logging with stack traces
     */
    @GetMapping("/exception/{type}")
    public ResponseEntity<Map<String, String>> demonstrateExceptionLogging(@PathVariable String type) {
        try {
            switch (type.toLowerCase()) {
                case "runtime" -> throw new RuntimeException("Simulated runtime exception");
                case "illegal" -> throw new IllegalArgumentException("Simulated illegal argument");
                case "null" -> throw new NullPointerException("Simulated null pointer");
                default -> log.info("No exception thrown for type: {}", type);
            }
        } catch (Exception e) {
            // Log exception with full stack trace
            log.error("Exception occurred for type '{}': {}", type, e.getMessage(), e);

            // Log exception with custom message
            log.error("Handling {} gracefully", type, e);
        }

        return ResponseEntity.ok(Map.of(
                "type", type,
                "message", "Exception logging demonstrated - check logs for stack trace"
        ));
    }

    /**
     * Demonstrates MDC (Mapped Diagnostic Context) usage
     */
    @GetMapping("/mdc/{userId}/{requestId}")
    public ResponseEntity<Map<String, String>> demonstrateMdc(
            @PathVariable String userId,
            @PathVariable String requestId) {

        // Add context to MDC - will appear in all subsequent log messages
        org.apache.logging.log4j.ThreadContext.put("userId", userId);
        org.apache.logging.log4j.ThreadContext.put("requestId", requestId);
        org.apache.logging.log4j.ThreadContext.put("operation", "MDC_DEMO");

        try {
            log.info("Processing request with MDC context");
            log.debug("This debug message also has MDC context");
            performBusinessLogic();
            log.info("Request processing completed");

            return ResponseEntity.ok(Map.of(
                    "userId", userId,
                    "requestId", requestId,
                    "message", "MDC logging demonstrated - check log pattern for context"
            ));
        } finally {
            // Always clear MDC to prevent context leaking to other requests
            org.apache.logging.log4j.ThreadContext.clearAll();
        }
    }

    /**
     * Demonstrates conditional logging with isEnabled checks
     */
    @GetMapping("/conditional")
    public ResponseEntity<Map<String, String>> demonstrateConditionalLogging() {
        // Check if log level is enabled before expensive operations
        if (log.isDebugEnabled()) {
            String expensiveData = gatherExpensiveDebugData();
            log.debug("Expensive debug data: {}", expensiveData);
        }

        if (log.isTraceEnabled()) {
            String veryExpensiveData = gatherVeryExpensiveTraceData();
            log.trace("Very expensive trace data: {}", veryExpensiveData);
        }

        // Using lambda for lazy evaluation (preferred approach)
        log.debug("Lazy evaluated data: {}", () -> gatherExpensiveDebugData());

        return ResponseEntity.ok(Map.of(
                "message", "Conditional logging demonstrated",
                "debugEnabled", String.valueOf(log.isDebugEnabled()),
                "traceEnabled", String.valueOf(log.isTraceEnabled())
        ));
    }

    private int performExpensiveCalculation(int count) {
        // Simulate expensive operation
        return count * count * count;
    }

    private void simulateWork() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void performBusinessLogic() {
        log.debug("Executing business logic step 1");
        log.debug("Executing business logic step 2");
        log.info("Business logic completed successfully");
    }

    private String gatherExpensiveDebugData() {
        return "Expensive debug data collected at " + System.currentTimeMillis();
    }

    private String gatherVeryExpensiveTraceData() {
        return "Very expensive trace data collected at " + System.nanoTime();
    }
}
