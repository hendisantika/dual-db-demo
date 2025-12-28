package id.my.hendisantika.dualdbdemo.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for HTTP client connection pool.
 * Optimized for high-traffic scenarios with Apache HttpClient 5.
 */
@Data
@ConfigurationProperties(prefix = "http.client.pool")
public class HttpClientPoolProperties {

    /**
     * Maximum total connections in the pool across all routes.
     * For high traffic: 200-500 depending on your server capacity.
     */
    private int maxTotal = 200;

    /**
     * Maximum connections per route (per host:port combination).
     * Should be sized based on expected concurrent requests to each host.
     */
    private int maxPerRoute = 50;

    /**
     * Connection timeout in milliseconds.
     * Time to wait for a connection to be established.
     */
    private long connectionTimeout = 5000;

    /**
     * Socket timeout (read timeout) in milliseconds.
     * Time to wait for data after connection is established.
     */
    private long socketTimeout = 30000;

    /**
     * Connection request timeout in milliseconds.
     * Time to wait when requesting a connection from the pool.
     */
    private long connectionRequestTimeout = 5000;

    /**
     * Time-to-live for connections in milliseconds.
     * Maximum lifespan of a connection in the pool.
     */
    private long connectionTimeToLive = 300000;

    /**
     * Idle connection timeout in milliseconds.
     * Connections idle longer than this will be evicted.
     */
    private long idleConnectionTimeout = 60000;

    /**
     * Validate connections after inactivity period in milliseconds.
     * Set to 0 to validate on every request (slower but safer).
     */
    private long validateAfterInactivity = 2000;

    /**
     * Enable connection eviction in background.
     */
    private boolean evictExpiredConnections = true;

    /**
     * Interval for evicting idle connections in milliseconds.
     */
    private long evictionInterval = 10000;
}
