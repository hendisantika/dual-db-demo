# Dual Database Demo

A Spring Boot application demonstrating CRUD operations with **dual database** support using **Spring Data JPA** with *
*HikariCP** for both **MySQL 9.5.0** and **PostgreSQL 18** with **automatic database connection failover** support.

This project also showcases **Project Loom** features available in JDK 25:

- **Virtual Threads** - Lightweight threads for high-throughput concurrent applications
- **Structured Concurrency** - Managing concurrent subtasks as a unit of work
- **Scoped Values** - Sharing immutable data across threads safely

## Features

- CRUD operations using Spring Data JPA
- Dual database configuration (MySQL & PostgreSQL)
- **Automatic database connection failover with multiple hosts**
- **Health monitoring with automatic failback to primary**
- HikariCP connection pooling for both databases
- **HTTP Client with Connection Pooling** using Apache HttpClient 5
- **Redis Fallback for HTTP Client** - cached response fallback when HTTP requests fail
- Health check endpoints via Spring Actuator
- Docker Compose setup for databases (primary & secondary instances)
- Sync product to both databases simultaneously
- **Virtual Threads** for all HTTP request handling
- **Structured Concurrency** for parallel database operations
- **Scoped Values** for request context propagation
- **Log4j2 Logging** with colored console output, rolling file appenders, and JSON structured logging
- **API Metrics Monitoring** with OpenTelemetry, Prometheus, and Grafana dashboards

## Tech Stack

- **Java 25** with Preview Features enabled
- Spring Boot 4.0.1
- Spring Framework 7.0.2
- Spring Web MVC (with Virtual Threads)
- Spring Data JPA
- Spring Data Redis
- HikariCP (Connection Pooling)
- **Apache HttpClient 5** (HTTP Client Connection Pooling)
- **Redis 7.4** (Caching & HTTP Fallback)
- **Log4j2** (Logging Framework)
- MySQL 9.5.0
- PostgreSQL 18
- Docker Compose
- Lombok (edge-SNAPSHOT for JDK 25 compatibility)
- **Project Loom**: Virtual Threads, Structured Concurrency, Scoped Values
- **Observability Stack**: Micrometer, Prometheus, Grafana

## Project Structure

```
src/main/java/id/my/hendisantika/dualdbdemo/
├── DualDbDemoApplication.java
├── config/
│   ├── MysqlJdbcConfig.java           # MySQL JDBC configuration with failover
│   ├── PostgresJdbcConfig.java        # PostgreSQL JDBC configuration with failover
│   ├── RestClientConfig.java          # HTTP Client with Apache HttpClient 5 pooling
│   ├── RedisConfig.java               # Redis configuration for fallback caching
│   └── properties/
│       ├── DatabaseHost.java          # Host configuration (host & port)
│       ├── HikariPoolProperties.java  # HikariCP connection pool settings
│       ├── FailoverProperties.java    # Failover configuration
│       ├── HttpClientPoolProperties.java # HTTP client pool settings
│       ├── MysqlProperties.java       # MySQL-specific properties
│       └── PostgresProperties.java    # PostgreSQL-specific properties
├── context/
│   └── RequestContext.java            # Scoped Values for request context
├── controller/
│   ├── LoggingDemoController.java     # Log4j2 logging demo endpoints
│   ├── MysqlProductController.java    # MySQL CRUD endpoints
│   ├── PostgresProductController.java # PostgreSQL CRUD endpoints
│   └── ProductController.java         # Combined endpoints
├── dto/
│   ├── ProductRequest.java
│   └── ProductResponse.java
├── entity/
│   ├── mysql/MysqlProduct.java
│   └── postgresql/PostgresProduct.java
├── repository/
│   ├── mysql/MysqlProductRepository.java
│   └── postgresql/PostgresProductRepository.java
└── service/
    ├── ProductService.java            # Uses Structured Concurrency + Scoped Values
    └── HttpClientService.java         # HTTP client with Redis fallback
```

## Prerequisites

- **Java 25** (JDK 25 required for Project Loom preview features)
- Docker & Docker Compose
- Maven

## Getting Started

### 1. Start the Infrastructure

```bash
docker compose up -d
```

This will start:
- MySQL 9.5.0 Primary on port `3308`
- MySQL 9.5.0 Secondary on port `3309` (failover)
- PostgreSQL 18 Primary on port `5433`
- PostgreSQL 18 Secondary on port `5434` (failover)
- Redis 7.4 on port `6379` (HTTP client fallback cache)
- Prometheus on port `9090` (metrics collection)
- Grafana on port `3000` (metrics visualization)

### 2. Run the Application

```bash
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`

### 3. Verify Health

```bash
curl http://localhost:8080/actuator/health
```

## Database Connection Failover

This application implements automatic database connection failover with multiple hosts. When the primary database becomes unavailable, the application will automatically attempt to connect to the secondary host.

### Failover Features

- **Automatic Host Switching**: When a connection fails, automatically tries the next configured host
- **Configurable Retry Logic**: Set maximum retries and delay between attempts
- **Health Monitoring**: Background scheduler periodically checks if primary host is back online
- **Automatic Failback**: When primary host recovers, connections automatically return to it
- **Connection Error Detection**: Identifies connection failures by SQL state and error messages

### Failover Configuration

Configuration is in `src/main/resources/application.properties`:

```properties
# MySQL Multiple Hosts for Failover
spring.datasource.mysql.hosts[0].host=localhost
spring.datasource.mysql.hosts[0].port=3308
spring.datasource.mysql.hosts[1].host=localhost
spring.datasource.mysql.hosts[1].port=3309
spring.datasource.mysql.database=profile-http
spring.datasource.mysql.username=yu71
spring.datasource.mysql.password=53cret
spring.datasource.mysql.failover.enabled=true
spring.datasource.mysql.failover.max-retries=3
spring.datasource.mysql.failover.retry-delay=1000
spring.datasource.mysql.failover.health-check-interval=30000

# PostgreSQL Multiple Hosts for Failover
spring.datasource.postgresql.hosts[0].host=localhost
spring.datasource.postgresql.hosts[0].port=5433
spring.datasource.postgresql.hosts[1].host=localhost
spring.datasource.postgresql.hosts[1].port=5434
spring.datasource.postgresql.database=profile_http
spring.datasource.postgresql.username=yu71
spring.datasource.postgresql.password=53cret
spring.datasource.postgresql.failover.enabled=true
spring.datasource.postgresql.failover.max-retries=3
spring.datasource.postgresql.failover.retry-delay=1000
spring.datasource.postgresql.failover.health-check-interval=30000
```

### Failover Properties

| Property                         | Description                                   | Default |
|----------------------------------|-----------------------------------------------|---------|
| `failover.enabled`               | Enable/disable failover mechanism             | `false` |
| `failover.max-retries`           | Maximum retry attempts before switching hosts | `3`     |
| `failover.retry-delay`           | Delay between retries in milliseconds         | `1000`  |
| `failover.health-check-interval` | Interval to check if primary is back (ms)     | `30000` |

### HikariCP Connection Pool Properties

| Property                  | Description                            | Default   |
|---------------------------|----------------------------------------|-----------|
| `pool.minimum-idle`       | Minimum number of idle connections     | `5`       |
| `pool.maximum-pool-size`  | Maximum number of connections          | `20`      |
| `pool.idle-timeout`       | Maximum idle time for connections (ms) | `30000`   |
| `pool.max-lifetime`       | Maximum lifetime for connections (ms)  | `1800000` |
| `pool.connection-timeout` | Connection timeout (ms)                | `30000`   |
| `pool.validation-timeout` | Validation timeout (ms)                | `5000`    |

## HTTP Client with Redis Fallback

This application includes an HTTP client service with built-in Redis fallback. When an HTTP request fails (due to network issues, server errors, etc.), the service automatically attempts to retrieve cached data from Redis.

### How It Works

```
HTTP Request → Success → Cache response to Redis → Return response
             ↓
           Failed → Lookup Redis cache → Found → Return cached data
                                       ↓
                                    Not Found → Throw original exception
```

### Features

- **Automatic Caching**: Successful HTTP responses are automatically cached in Redis
- **Transparent Fallback**: When HTTP fails, cached data is returned seamlessly
- **Configurable TTL**: Cache expiration time is configurable
- **Connection Pooling**: Apache HttpClient 5 with optimized connection pool
- **Context Propagation**: Correlation ID and User ID headers are automatically propagated

### Configuration

```properties
# Redis Configuration
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.database=0
spring.data.redis.timeout=5000
spring.data.redis.lettuce.pool.enabled=true
spring.data.redis.lettuce.pool.max-active=10
spring.data.redis.lettuce.pool.max-idle=5
spring.data.redis.lettuce.pool.min-idle=2

# HTTP Client Fallback Configuration
http.client.fallback.enabled=true
http.client.fallback.cache-ttl-minutes=30

# HTTP Client Connection Pool Configuration
http.client.pool.max-total=200
http.client.pool.max-per-route=50
http.client.pool.connection-timeout=5000
http.client.pool.socket-timeout=30000
http.client.pool.connection-request-timeout=5000
http.client.pool.connection-time-to-live=300000
http.client.pool.idle-connection-timeout=60000
http.client.pool.validate-after-inactivity=2000
http.client.pool.evict-expired-connections=true
http.client.pool.eviction-interval=10000
```

### HTTP Client Pool Properties

| Property                          | Description                                | Default   |
|-----------------------------------|-------------------------------------------|-----------|
| `pool.max-total`                  | Maximum total connections in pool         | `200`     |
| `pool.max-per-route`              | Maximum connections per route             | `50`      |
| `pool.connection-timeout`         | Connection timeout (ms)                   | `5000`    |
| `pool.socket-timeout`             | Socket read timeout (ms)                  | `30000`   |
| `pool.connection-request-timeout` | Time to wait for connection from pool (ms)| `5000`    |
| `pool.connection-time-to-live`    | Maximum connection lifetime (ms)          | `300000`  |
| `pool.idle-connection-timeout`    | Idle connection timeout (ms)              | `60000`   |
| `pool.validate-after-inactivity`  | Validate connection after inactivity (ms) | `2000`    |

### Fallback Properties

| Property                          | Description                           | Default |
|-----------------------------------|---------------------------------------|---------|
| `fallback.enabled`                | Enable/disable Redis fallback         | `true`  |
| `fallback.cache-ttl-minutes`      | Cache TTL in minutes                  | `30`    |

### Usage Example

```java
@Autowired
private HttpClientService httpClientService;

// Simple GET request with automatic caching and fallback
String response = httpClientService.get("https://api.example.com/data", String.class);

// GET with path variables
Product product = httpClientService.get("https://api.example.com/products/{id}", Product.class, 123);

// GET with query parameters
Map<String, String> params = Map.of("category", "electronics", "limit", "10");
List<Product> products = httpClientService.getWithParams("https://api.example.com/products", ProductList.class, params);

// Manual cache operations
httpClientService.cacheValue("https://api.example.com/data", myData);
httpClientService.invalidateCache("https://api.example.com/data");
```

### Expected Log Output

When HTTP request succeeds:
```
DEBUG HttpClientService - Executing GET request to: https://api.example.com/data
DEBUG HttpClientService - Cached response for key: http:cache:123456789
```

When HTTP request fails but cache is available:
```
WARN  HttpClientService - HTTP request failed for URL: https://api.example.com/data. Error: Connection refused
INFO  HttpClientService - HTTP request failed, returning cached data for key: http:cache:123456789
```

When HTTP request fails and no cache:
```
WARN  HttpClientService - HTTP request failed for URL: https://api.example.com/data. Error: Connection refused
WARN  HttpClientService - No cached data available for key: http:cache:123456789
```

## Project Loom Features (JDK 25)

This application demonstrates three key Project Loom features that revolutionize concurrent programming in Java.

### Virtual Threads

Virtual Threads are lightweight threads managed by the JVM, enabling high-throughput concurrent applications without the
overhead of platform threads.

**Configuration** (`application.properties`):

```properties
# Enable Virtual Threads for Spring MVC
spring.threads.virtual.enabled=true
```

**Benefits:**

- Each HTTP request runs on a virtual thread
- Can handle millions of concurrent connections
- No thread pool tuning required
- Blocking I/O doesn't waste resources

### Structured Concurrency

Structured Concurrency treats multiple concurrent tasks as a single unit of work, ensuring proper cleanup and error
handling.

**Example** (`ProductService.java`):

```java
public List<ProductResponse> getAllProductsFromBothDatabases() {
    try (var scope = StructuredTaskScope.open()) {
        // Fork parallel tasks - both inherit virtual thread context
        var mysqlTask = scope.fork(this::getAllMysqlProducts);
        var postgresTask = scope.fork(this::getAllPostgresProducts);

        // Wait for both to complete (or one to fail)
        scope.join();

        // Merge results
        return Stream.concat(
                mysqlTask.get().stream(),
                postgresTask.get().stream()
        ).toList();
    }
}
```

**Benefits:**

- Automatic cancellation if one task fails
- Guaranteed cleanup when scope closes
- Clear parent-child relationship in thread dumps
- No orphaned threads

### Scoped Values

Scoped Values provide a safe way to share immutable data across threads, replacing `ThreadLocal` for virtual thread
scenarios.

**Definition** (`RequestContext.java`):

```java
public final class RequestContext {
    public static final ScopedValue<String> CORRELATION_ID = ScopedValue.newInstance();
    public static final ScopedValue<String> OPERATION = ScopedValue.newInstance();

    public static String getCorrelationId() {
        return CORRELATION_ID.orElse("unknown");
    }
}
```

**Usage** (`ProductService.java`):

```java
public List<ProductResponse> getAllProductsFromBothDatabases() {
    String correlationId = UUID.randomUUID().toString().substring(0, 8);

    return ScopedValue.where(RequestContext.CORRELATION_ID, correlationId)
            .where(RequestContext.OPERATION, "getAllProductsFromBothDatabases")
            .call(() -> {
                // All forked subtasks automatically inherit these values!
                try (var scope = StructuredTaskScope.open()) {
                    scope.fork(() -> {
                        // Can access RequestContext.getCorrelationId() here
                        log.debug("[{}] Fetching from MySQL", RequestContext.getCorrelationId());
                        return getAllMysqlProducts();
                    });
                    // ...
                }
            });
}
```

**Benefits over ThreadLocal:**

- Immutable by design (thread-safe)
- Automatically inherited by child threads in `StructuredTaskScope`
- More efficient memory usage with virtual threads
- Clear lifecycle boundaries

### Observing Project Loom in Action

When you call the `/api/products/all` endpoint, you'll see logs like:

```
[a1b2c3d4] Starting parallel fetch from both databases
[a1b2c3d4] Fetching from MySQL
[a1b2c3d4] Fetching from PostgreSQL
[a1b2c3d4] Successfully fetched products from both databases
```

The correlation ID (`a1b2c3d4`) is propagated to all subtasks via Scoped Values, making distributed tracing easy!

## API Metrics Monitoring with OpenTelemetry, Prometheus & Grafana

This application includes comprehensive API metrics monitoring using Micrometer (OpenTelemetry-compatible), Prometheus, and Grafana. Track request counts, response times, and visualize API performance in real-time.

### What's Being Tracked

The application automatically tracks:

- **Request Count**: Total number of hits per API endpoint
- **Response Time**: Latency percentiles (p50, p95, p99)
- **HTTP Status Codes**: Distribution of 2xx, 4xx, 5xx responses
- **Request Rate**: Requests per second for each endpoint
- **Endpoint-Specific Metrics**: Dedicated counters for `/api/products/*` endpoints

### Quick Start

1. **Start Prometheus & Grafana**:
   ```bash
   docker compose up -d prometheus grafana
   ```

2. **Start the Application**:
   ```bash
   mvn spring-boot:run
   ```

3. **Generate Traffic** (optional):
   ```bash
   ./test-metrics.sh
   ```

4. **View Dashboard**:
   - Open Grafana: http://localhost:3000 (admin/admin)
   - Navigate to: **Dashboards** → **Dual DB Demo - API Metrics**

### Access Points

| Service          | URL                                       | Description                    |
|------------------|-------------------------------------------|--------------------------------|
| Grafana          | http://localhost:3000                     | Metrics dashboards (admin/admin)|
| Prometheus       | http://localhost:9090                     | Metrics database & query UI    |
| Actuator Metrics | http://localhost:8080/actuator/prometheus | Raw metrics endpoint           |

### Dashboard Features

The pre-configured Grafana dashboard includes:

1. **Request Rate Graph**: Real-time requests/second for `/api/products/all`
2. **Total Hits Counter**: Cumulative hit count for each endpoint
3. **Response Time Percentiles**: p50, p95, p99 latency trends
4. **Status Distribution**: Pie chart showing HTTP status codes
5. **Endpoints Summary Table**: All API endpoints with hit counts
6. **Request Rate by Endpoint**: Stacked area chart of all product APIs

### Example Prometheus Queries

```promql
# Total hits for /api/products/all
sum(api_products_hits_total{endpoint="/api/products/all"})

# Request rate (requests per second)
rate(api_products_hits_total[1m])

# P95 response time
histogram_quantile(0.95,
  sum(rate(api_request_duration_milliseconds_bucket[5m])) by (le)
)
```

### Testing the Metrics

Use the included test script to generate API traffic:

```bash
# Make executable
chmod +x test-metrics.sh

# Run interactive menu
./test-metrics.sh
```

Options include:
- Quick test (50 GET requests)
- Load test (1000 requests)
- Create products (POST requests)
- Mixed traffic scenarios
- Continuous load testing

### Manual Testing

```bash
# Generate 100 hits to /api/products/all
for i in {1..100}; do
  curl -s http://localhost:8080/api/products/all > /dev/null
  echo -n "."
done

# Check metrics in Prometheus
open http://localhost:9090/graph?g0.expr=api_products_hits_total
```

### Custom Metrics Configuration

Metrics are configured in src/main/java/id/my/hendisantika/dualdbdemo/config/MetricsConfig.java:571:13

- **ApiMetricsInterceptor**: Intercepts all HTTP requests
- **Counters**: Track request counts by URI, method, and status
- **Timers**: Measure request duration with histogram buckets

### Detailed Guide

For comprehensive setup instructions, troubleshooting, and advanced features, see:
- [METRICS_GUIDE.md](METRICS_GUIDE.md) - Complete metrics monitoring guide

## Log4j2 Logging

This application uses **Log4j2** as the logging framework, replacing the default Logback. Log4j2 provides better
performance, more features, and flexibility for enterprise applications.

### Log4j2 Features

- **Colored Console Output**: Different colors for each log level (INFO=green, DEBUG=cyan, WARN=yellow, ERROR=red,
  FATAL=red blink)
- **Rolling File Appenders**: Automatic log rotation by size (10MB) and time (daily)
- **Error-Only Log File**: Separate file capturing only ERROR and FATAL level logs
- **JSON Structured Logging**: Machine-readable JSON format for log aggregation systems
- **Parameterized Logging**: Efficient string handling without concatenation overhead
- **Marker-Based Logging**: Categorize logs (PERFORMANCE, SECURITY, BUSINESS)
- **MDC Support**: Mapped Diagnostic Context for request tracing

### Log Files

Log files are created in the `logs/` directory:

| File                     | Description                |
|--------------------------|----------------------------|
| `dual-db-demo.log`       | All application logs       |
| `dual-db-demo-error.log` | ERROR and FATAL level only |
| `dual-db-demo.json`      | JSON structured logs       |

### Log4j2 Configuration

Configuration is in `src/main/resources/log4j2.xml`:

```xml
<Configuration status="WARN" monitorInterval="30">
    <Appenders>
        <!-- Colored console output -->
        <Console name="Console" target="SYSTEM_OUT">
            <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss.SSS} %highlight{%-5level} [%style{%t}{bright,blue}] %style{%logger{36}}{cyan} - %msg%n"/>
        </Console>

        <!-- Rolling file with daily rotation -->
        <RollingFile name="RollingFile" fileName="logs/dual-db-demo.log"
                     filePattern="logs/dual-db-demo-%d{yyyy-MM-dd}-%i.log.gz">
            <Policies>
                <TimeBasedTriggeringPolicy interval="1"/>
                <SizeBasedTriggeringPolicy size="10MB"/>
            </Policies>
        </RollingFile>
    </Appenders>

    <Loggers>
        <Logger name="id.my.hendisantika.dualdbdemo" level="DEBUG"/>
        <Root level="INFO">
            <AppenderRef ref="Console"/>
            <AppenderRef ref="RollingFile"/>
        </Root>
    </Loggers>
</Configuration>
```

### Logging Demo Endpoints

The application includes demo endpoints showcasing Log4j2 features:

| Method | Endpoint                                    | Description                                                          |
|--------|---------------------------------------------|----------------------------------------------------------------------|
| GET    | `/api/logging/levels`                       | Demonstrates all log levels (TRACE, DEBUG, INFO, WARN, ERROR, FATAL) |
| GET    | `/api/logging/parameterized/{name}/{count}` | Parameterized logging (efficient)                                    |
| GET    | `/api/logging/markers/{action}`             | Marker-based logging (PERFORMANCE, SECURITY, BUSINESS)               |
| GET    | `/api/logging/exception/{type}`             | Exception logging with stack traces                                  |
| GET    | `/api/logging/mdc/{userId}/{requestId}`     | MDC (Mapped Diagnostic Context) usage                                |
| GET    | `/api/logging/conditional`                  | Conditional logging with isEnabled checks                            |

### Logging Demo Examples

```bash
# Test all log levels
curl http://localhost:8080/api/logging/levels

# Test parameterized logging
curl http://localhost:8080/api/logging/parameterized/TestUser/5

# Test marker-based logging
curl http://localhost:8080/api/logging/markers/purchase

# Test exception logging
curl http://localhost:8080/api/logging/exception/runtime

# Test MDC context
curl http://localhost:8080/api/logging/mdc/user123/req456

# Test conditional logging
curl http://localhost:8080/api/logging/conditional
```

### Sample Log Output

```
2025-12-19 10:17:48.749 DEBUG [tomcat-handler-0] id.my.hendisantika.dualdbdemo.controller.LoggingDemoController - This is a DEBUG level message
2025-12-19 10:17:48.749 INFO  [tomcat-handler-0] id.my.hendisantika.dualdbdemo.controller.LoggingDemoController - This is an INFO level message
2025-12-19 10:17:48.749 WARN  [tomcat-handler-0] id.my.hendisantika.dualdbdemo.controller.LoggingDemoController - This is a WARN level message
2025-12-19 10:17:48.749 ERROR [tomcat-handler-0] id.my.hendisantika.dualdbdemo.controller.LoggingDemoController - This is an ERROR level message
2025-12-19 10:17:48.750 FATAL [tomcat-handler-0] id.my.hendisantika.dualdbdemo.controller.LoggingDemoController - This is a FATAL level message
```

## API Endpoints

### MySQL Products

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/mysql/products` | Create a product |
| GET | `/api/mysql/products` | Get all products |
| GET | `/api/mysql/products/{id}` | Get product by ID |
| PUT | `/api/mysql/products/{id}` | Update a product |
| DELETE | `/api/mysql/products/{id}` | Delete a product |
| GET | `/api/mysql/products/search?name=` | Search products by name |

### PostgreSQL Products

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/postgres/products` | Create a product |
| GET | `/api/postgres/products` | Get all products |
| GET | `/api/postgres/products/{id}` | Get product by ID |
| PUT | `/api/postgres/products/{id}` | Update a product |
| DELETE | `/api/postgres/products/{id}` | Delete a product |
| GET | `/api/postgres/products/search?name=` | Search products by name |

### Combined Operations

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/products/all` | Get all products from both databases |
| POST | `/api/products/sync` | Create product in both databases |

### Logging Demo

| Method | Endpoint                                    | Description                |
|--------|---------------------------------------------|----------------------------|
| GET    | `/api/logging/levels`                       | Demonstrate all log levels |
| GET    | `/api/logging/parameterized/{name}/{count}` | Parameterized logging      |
| GET    | `/api/logging/markers/{action}`             | Marker-based logging       |
| GET    | `/api/logging/exception/{type}`             | Exception logging          |
| GET    | `/api/logging/mdc/{userId}/{requestId}`     | MDC context logging        |
| GET    | `/api/logging/conditional`                  | Conditional logging        |

## Curl Examples

### Health Check

```bash
# Check application health
curl -s http://localhost:8080/actuator/health | jq

# Check all actuator endpoints
curl -s http://localhost:8080/actuator | jq
```

### Create Products

```bash
# Create a product in MySQL
curl -X POST http://localhost:8080/api/mysql/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "MacBook Pro 16",
    "description": "Apple MacBook Pro with M3 chip",
    "price": 2499.99,
    "quantity": 50
  }'

# Create a product in PostgreSQL
curl -X POST http://localhost:8080/api/postgres/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "iPhone 15 Pro",
    "description": "Apple iPhone 15 Pro 256GB",
    "price": 1199.99,
    "quantity": 100
  }'

# Sync product to BOTH databases simultaneously
curl -X POST http://localhost:8080/api/products/sync \
  -H "Content-Type: application/json" \
  -d '{
    "name": "AirPods Pro",
    "description": "Apple AirPods Pro 2nd Generation",
    "price": 249.99,
    "quantity": 200
  }'
```

### Read Products

```bash
# Get all MySQL products
curl -s http://localhost:8080/api/mysql/products | jq

# Get all PostgreSQL products
curl -s http://localhost:8080/api/postgres/products | jq

# Get all products from BOTH databases
curl -s http://localhost:8080/api/products/all | jq

# Get specific product by ID (MySQL)
curl -s http://localhost:8080/api/mysql/products/1 | jq

# Get specific product by ID (PostgreSQL)
curl -s http://localhost:8080/api/postgres/products/1 | jq

# Search products by name (MySQL)
curl -s "http://localhost:8080/api/mysql/products/search?name=MacBook" | jq

# Search products by name (PostgreSQL)
curl -s "http://localhost:8080/api/postgres/products/search?name=iPhone" | jq
```

### Update Products

```bash
# Update a MySQL product
curl -X PUT http://localhost:8080/api/mysql/products/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "MacBook Pro 16 (Updated)",
    "description": "Apple MacBook Pro with M3 Max chip",
    "price": 3499.99,
    "quantity": 25
  }'

# Update a PostgreSQL product
curl -X PUT http://localhost:8080/api/postgres/products/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "iPhone 15 Pro Max",
    "description": "Apple iPhone 15 Pro Max 512GB",
    "price": 1399.99,
    "quantity": 75
  }'
```

### Delete Products

```bash
# Delete a MySQL product
curl -X DELETE http://localhost:8080/api/mysql/products/1

# Delete a PostgreSQL product
curl -X DELETE http://localhost:8080/api/postgres/products/1
```

## Testing Failover

### Using the Test Script

A comprehensive test script is provided for testing failover scenarios:

```bash
# Make the script executable
chmod +x test-failover.sh

# Show available commands
./test-failover.sh

# Test basic CRUD operations
./test-failover.sh basic

# Test MySQL failover
./test-failover.sh mysql

# Test PostgreSQL failover
./test-failover.sh postgres

# Test dual database sync
./test-failover.sh sync

# Run all tests
./test-failover.sh all

# Check container status
./test-failover.sh status
```

### Manual Failover Testing

#### Step 1: Start all databases and the application

```bash
# Start all database containers
docker compose up -d

# Wait for databases to be ready
sleep 10

# Start the application
./mvnw spring-boot:run
```

#### Step 2: Verify everything is working

```bash
# Create a test product in MySQL
curl -X POST http://localhost:8080/api/mysql/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Before Failover","description":"Test","price":10.00,"quantity":5}'

# Verify the product was created
curl -s http://localhost:8080/api/mysql/products | jq
```

#### Step 3: Test MySQL Failover

```bash
# Stop the primary MySQL database
docker compose stop mysql-primary

# Wait a moment for the failover to kick in
sleep 3

# Try to create another product (should failover to secondary)
curl -X POST http://localhost:8080/api/mysql/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Test During Failover","description":"Created on secondary","price":20.00,"quantity":10}'

# Verify the product was created on secondary
curl -s http://localhost:8080/api/mysql/products | jq

# Check application logs to see failover messages
# You should see: "MySQL connection failed, attempting failover"
# And: "MySQL switching to host: localhost:3309"
```

#### Step 4: Test Automatic Failback

```bash
# Restart the primary MySQL database
docker compose start mysql-primary

# Wait for health check to detect primary is back (default: 30 seconds)
sleep 35

# Create another product (should be back on primary)
curl -X POST http://localhost:8080/api/mysql/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Test After Failback","description":"Back on primary","price":30.00,"quantity":15}'

# Check application logs to see failback messages
# You should see: "MySQL primary host localhost:3308 is back online, switching back"
```

#### Step 5: Test PostgreSQL Failover

```bash
# Stop the primary PostgreSQL database
docker compose stop postgres-primary

# Wait a moment for the failover to kick in
sleep 3

# Try to create a product (should failover to secondary)
curl -X POST http://localhost:8080/api/postgres/products \
  -H "Content-Type: application/json" \
  -d '{"name":"PostgreSQL Failover Test","description":"Created on secondary","price":50.00,"quantity":25}'

# Verify the product was created
curl -s http://localhost:8080/api/postgres/products | jq

# Restart the primary PostgreSQL
docker compose start postgres-primary
```

#### Step 6: Test Both Databases Simultaneously

```bash
# Stop both primary databases
docker compose stop mysql-primary postgres-primary

# Wait for failover
sleep 3

# Sync a product to both databases (both should use secondaries)
curl -X POST http://localhost:8080/api/products/sync \
  -H "Content-Type: application/json" \
  -d '{"name":"Dual Failover Test","description":"On both secondaries","price":100.00,"quantity":50}'

# Verify products in both databases
curl -s http://localhost:8080/api/products/all | jq

# Restart both primaries
docker compose start mysql-primary postgres-primary
```

### Expected Log Output During Failover

When failover occurs, you should see logs like:

```
WARN  MysqlJdbcConfig : MySQL connection failed (attempt 1): Connection refused
INFO  MysqlJdbcConfig : MySQL switching to host: localhost:3309
DEBUG MysqlJdbcConfig : Created MySQL product: 5

# When primary comes back:
INFO  MysqlJdbcConfig : MySQL primary host localhost:3308 is back online, switching back
```

## Docker Commands

```bash
# Start all databases (primary & secondary)
docker compose up -d

# Stop all databases
docker compose down

# Stop only primary MySQL (to test failover)
docker compose stop mysql-primary

# Start primary MySQL again
docker compose start mysql-primary

# Stop only primary PostgreSQL (to test failover)
docker compose stop postgres-primary

# Start primary PostgreSQL again
docker compose start postgres-primary

# View logs
docker compose logs -f

# View logs for specific service
docker compose logs -f mysql-primary

# Check container status
docker compose ps

# Restart all containers
docker compose restart
```

## Running Integration Tests

```bash
# Run all tests (requires databases to be running)
./mvnw test

# Run specific test class
./mvnw test -Dtest=FailoverIntegrationTest

# Run tests with debug output
./mvnw test -X
```

## Troubleshooting

### Connection Refused Errors

If you see "Connection refused" errors:

1. Ensure Docker containers are running: `docker compose ps`
2. Check if ports are available: `lsof -i :3308` and `lsof -i :5433`
3. Verify database credentials in `application.properties`

### Failover Not Working

If failover is not working:

1. Ensure `failover.enabled=true` in configuration
2. Check that secondary hosts are configured correctly
3. Verify secondary database containers are running
4. Check application logs for failover messages

### Health Check Not Detecting Primary Recovery

If the application doesn't failback to primary:

1. Increase `health-check-interval` if primary takes longer to restart
2. Check that primary is fully ready (not just started)
3. Verify primary database is accessible from the application

## License

MIT
