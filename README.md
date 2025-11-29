# Dual Database Demo

A Spring Boot application demonstrating CRUD operations with **dual database** support using **R2DBC** (Reactive Relational Database Connectivity) for both **MySQL 9.5.0** and **PostgreSQL 18** with **database connection failover** support.

## Features

- Reactive CRUD operations using Spring WebFlux and R2DBC
- Dual database configuration (MySQL & PostgreSQL)
- **Database connection failover with multiple hosts**
- Connection pooling for both databases
- Health check endpoints via Spring Actuator
- Docker Compose setup for databases (primary & secondary instances)
- Sync product to both databases simultaneously

## Tech Stack

- Java 24 (compatible with JDK 25 when available)
- Spring Boot 4.0.0
- Spring Framework 7.0.1
- Spring WebFlux
- Spring Data R2DBC
- MySQL 9.5.0
- PostgreSQL 18
- Docker Compose
- Lombok (edge-SNAPSHOT for JDK 24+ compatibility)

## Project Structure

```
src/main/java/id/my/hendisantika/dualdbdemo/
├── DualDbDemoApplication.java
├── config/
│   ├── MysqlR2dbcConfig.java         # MySQL R2DBC configuration with failover
│   ├── PostgresR2dbcConfig.java      # PostgreSQL R2DBC configuration with failover
│   └── properties/
│       ├── DatabaseHost.java          # Host configuration (host & port)
│       ├── PoolProperties.java        # Connection pool settings
│       ├── FailoverProperties.java    # Failover configuration
│       ├── MysqlProperties.java       # MySQL-specific properties
│       └── PostgresProperties.java    # PostgreSQL-specific properties
├── controller/
│   ├── MysqlProductController.java   # MySQL CRUD endpoints
│   ├── PostgresProductController.java # PostgreSQL CRUD endpoints
│   └── ProductController.java        # Combined endpoints
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

### Failover Configuration

Configuration is in `src/main/resources/application.properties`:

```properties
# MySQL Multiple Hosts for Failover
spring.r2dbc.mysql.hosts[0].host=localhost
spring.r2dbc.mysql.hosts[0].port=3308
spring.r2dbc.mysql.hosts[1].host=localhost
spring.r2dbc.mysql.hosts[1].port=3309
spring.r2dbc.mysql.database=profile-http
spring.r2dbc.mysql.username=yu71
spring.r2dbc.mysql.password=53cret
spring.r2dbc.mysql.failover.enabled=true
spring.r2dbc.mysql.failover.max-retries=3
spring.r2dbc.mysql.failover.retry-delay=1000

# PostgreSQL Multiple Hosts for Failover
spring.r2dbc.postgresql.hosts[0].host=localhost
spring.r2dbc.postgresql.hosts[0].port=5433
spring.r2dbc.postgresql.hosts[1].host=localhost
spring.r2dbc.postgresql.hosts[1].port=5434
spring.r2dbc.postgresql.database=profile_http
spring.r2dbc.postgresql.username=yu71
spring.r2dbc.postgresql.password=53cret
spring.r2dbc.postgresql.failover.enabled=true
spring.r2dbc.postgresql.failover.max-retries=3
spring.r2dbc.postgresql.failover.retry-delay=1000
```

### Failover Properties

| Property | Description | Default |
|----------|-------------|---------|
| `failover.enabled` | Enable/disable failover mechanism | `false` |
| `failover.max-retries` | Maximum retry attempts before switching hosts | `3` |
| `failover.retry-delay` | Delay between retries in milliseconds | `1000` |

### Connection Pool Properties

| Property | Description | Default |
|----------|-------------|---------|
| `pool.initial-size` | Initial number of connections | `5` |
| `pool.max-size` | Maximum number of connections | `20` |
| `pool.max-idle-time` | Maximum idle time for connections | `30m` |
| `pool.max-life-time` | Maximum lifetime for connections | `60m` |
| `pool.validation-query` | Query to validate connections | `SELECT 1` |

### Testing Failover

To test the failover mechanism:

1. Start all database containers:
   ```bash
   docker compose up -d
   ```

2. Create a product to verify connection:
   ```bash
   curl -X POST http://localhost:8080/api/mysql/products \
     -H "Content-Type: application/json" \
     -d '{"name": "Test", "description": "Test", "price": 10, "quantity": 5}'
   ```

3. Stop the primary MySQL:
   ```bash
   docker compose stop mysql-primary
   ```

4. The application will automatically attempt to failover to the secondary host.

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

## Example API Calls

### Create a Product (MySQL)

```bash
curl -X POST http://localhost:8080/api/mysql/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Sample Product",
    "description": "A sample product description",
    "price": 99.99,
    "quantity": 10
  }'
```

### Create a Product (PostgreSQL)

```bash
curl -X POST http://localhost:8080/api/postgres/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Sample Product",
    "description": "A sample product description",
    "price": 99.99,
    "quantity": 10
  }'
```

### Sync Product to Both Databases

```bash
curl -X POST http://localhost:8080/api/products/sync \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Synced Product",
    "description": "Available in both databases",
    "price": 149.99,
    "quantity": 25
  }'
```

### Get All Products from Both Databases

```bash
curl http://localhost:8080/api/products/all
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

# View logs
docker compose logs -f

# Check container status
docker compose ps
```

## License

MIT
