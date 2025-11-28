# Dual Database Demo

A Spring Boot application demonstrating CRUD operations with **dual database** support using **R2DBC** (Reactive Relational Database Connectivity) for both **MySQL 9.5.0** and **PostgreSQL 18**.

## Features

- Reactive CRUD operations using Spring WebFlux and R2DBC
- Dual database configuration (MySQL & PostgreSQL)
- Connection pooling for both databases
- Health check endpoints via Spring Actuator
- Docker Compose setup for databases
- Sync product to both databases simultaneously

## Tech Stack

- Java 25
- Spring Boot 4.0.0
- Spring WebFlux
- Spring Data R2DBC
- MySQL 9.5.0
- PostgreSQL 18
- Docker Compose
- Lombok

## Project Structure

```
src/main/java/id/my/hendisantika/dualdbdemo/
├── DualDbDemoApplication.java
├── config/
│   ├── MysqlR2dbcConfig.java         # MySQL R2DBC configuration
│   └── PostgresR2dbcConfig.java      # PostgreSQL R2DBC configuration
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

- Java 25
- Docker & Docker Compose
- Maven

## Getting Started

### 1. Start the Databases

```bash
docker compose up -d
```

This will start:
- MySQL 9.5.0 on port `3308`
- PostgreSQL 18 on port `5433`

### 2. Run the Application

```bash
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`

### 3. Verify Health

```bash
curl http://localhost:8080/actuator/health
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

## Database Configuration

Configuration is in `src/main/resources/application.properties`:

```properties
# MySQL Configuration
spring.r2dbc.mysql.url=r2dbc:mysql://localhost:3308/profile-http
spring.r2dbc.mysql.username=yu71
spring.r2dbc.mysql.password=53cret

# PostgreSQL Configuration
spring.r2dbc.postgresql.url=r2dbc:postgresql://localhost:5433/profile_http
spring.r2dbc.postgresql.username=yu71
spring.r2dbc.postgresql.password=53cret
```

## Docker Commands

```bash
# Start databases
docker compose up -d

# Stop databases
docker compose down

# View logs
docker compose logs -f

# Check container status
docker compose ps
```

## License

MIT
