# Dual Database Demo

A Spring Boot application demonstrating CRUD operations with **dual database** support using **Spring Data JPA** with *
*HikariCP** for both **MySQL 9.5.0** and **PostgreSQL 18** with **automatic database connection failover** support.

## Features

- CRUD operations using Spring Data JPA
- Dual database configuration (MySQL & PostgreSQL)
- **Automatic database connection failover with multiple hosts**
- **Health monitoring with automatic failback to primary**
- HikariCP connection pooling for both databases
- Health check endpoints via Spring Actuator
- Docker Compose setup for databases (primary & secondary instances)
- Sync product to both databases simultaneously

## Tech Stack

- Java 24 (compatible with JDK 25 when available)
- Spring Boot 4.0.0
- Spring Framework 7.0.1
- Spring Web MVC
- Spring Data JPA
- HikariCP (Connection Pooling)
- MySQL 9.5.0
- PostgreSQL 18
- Docker Compose
- Lombok (edge-SNAPSHOT for JDK 24+ compatibility)

## Project Structure

```
src/main/java/id/my/hendisantika/dualdbdemo/
├── DualDbDemoApplication.java
├── config/
│   ├── MysqlJdbcConfig.java           # MySQL JDBC configuration with failover
│   ├── PostgresJdbcConfig.java        # PostgreSQL JDBC configuration with failover
│   └── properties/
│       ├── DatabaseHost.java          # Host configuration (host & port)
│       ├── HikariPoolProperties.java  # HikariCP connection pool settings
│       ├── FailoverProperties.java    # Failover configuration
│       ├── MysqlProperties.java       # MySQL-specific properties
│       └── PostgresProperties.java    # PostgreSQL-specific properties
├── controller/
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
    └── ProductService.java
```

## Prerequisites

- Java 24+ (JDK 24 or higher required for Spring Boot 4)
- Docker & Docker Compose
- Maven

## Getting Started

### 1. Start the Databases

```bash
docker compose up -d
```

This will start:
- MySQL 9.5.0 Primary on port `3308`
- MySQL 9.5.0 Secondary on port `3309` (failover)
- PostgreSQL 18 Primary on port `5433`
- PostgreSQL 18 Secondary on port `5434` (failover)

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
