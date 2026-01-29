# API Metrics with OpenTelemetry, Prometheus, and Grafana

This guide explains how to track API request hits and visualize them on Grafana dashboard.

## Architecture

The monitoring stack consists of:

1. **Spring Boot Application** - Exposes metrics via Micrometer and Actuator
2. **Prometheus** - Scrapes and stores metrics from the application
3. **Grafana** - Visualizes metrics with dashboards

## Metrics Collected

The application automatically tracks:

- **api.request.count** - Total number of requests per endpoint
- **api.products.hits** - Specific counter for `/api/products/*` endpoints
- **api.request.duration** - Request duration/latency with percentiles (p50, p95, p99)
- **HTTP status codes** - Response status distribution
- **Request method** - GET, POST, etc.

## Setup Instructions

### 1. Start the Infrastructure

Start Prometheus and Grafana along with the databases:

```bash
docker-compose up -d prometheus grafana
```

Verify the services are running:

```bash
docker-compose ps
```

### 2. Build and Start the Application

```bash
mvn clean package
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### 3. Access Monitoring Tools

- **Application Actuator**: http://localhost:8080/actuator
- **Prometheus Metrics**: http://localhost:8080/actuator/prometheus
- **Prometheus UI**: http://localhost:9090
- **Grafana Dashboard**: http://localhost:3000

### 4. Grafana Login

Default credentials:
- Username: `admin`
- Password: `admin`

## Accessing the Dashboard

1. Open Grafana at http://localhost:3000
2. Login with admin/admin
3. Navigate to **Dashboards** → **Dual DB Demo - API Metrics**

The dashboard is automatically provisioned and includes:

- **Request Rate**: Real-time requests per second for `/api/products/all`
- **Total Hits**: Cumulative hit counter
- **Response Time Percentiles**: p50, p95, p99 latency
- **Request Rate by Endpoint**: All product API endpoints
- **Status Distribution**: Pie chart of HTTP status codes
- **Endpoints Summary**: Table view of all endpoints with hit counts

## Testing the Metrics

### Generate API Traffic

```bash
# Hit the /api/products/all endpoint multiple times
for i in {1..100}; do
  curl -X GET http://localhost:8080/api/products/all
  sleep 0.1
done
```

### Create Products

```bash
# Sync a product to both databases
curl -X POST http://localhost:8080/api/products/sync \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Product",
    "description": "A test product",
    "price": 99.99
  }'
```

## Query Metrics in Prometheus

Access Prometheus UI at http://localhost:9090 and try these queries:

### Total hits for /api/products/all
```promql
sum(api_products_hits_total{endpoint="/api/products/all"})
```

### Request rate (requests per second)
```promql
rate(api_products_hits_total{endpoint="/api/products/all"}[1m])
```

### Average response time
```promql
rate(api_request_duration_milliseconds_sum{uri="/api/products/all"}[5m])
/
rate(api_request_duration_milliseconds_count{uri="/api/products/all"}[5m])
```

### Requests by HTTP status
```promql
sum by(status) (api_request_count_total{uri=~"/api/products.*"})
```

### P95 latency
```promql
histogram_quantile(0.95,
  sum(rate(api_request_duration_milliseconds_bucket{uri="/api/products/all"}[5m])) by (le)
)
```

## Customizing Metrics

### Add Custom Metrics

To track additional endpoints or add custom metrics, edit `MetricsConfig.java`:

```java
// Example: Add a gauge for active database connections
meterRegistry.gauge("database.connections.active",
    connectionPool,
    ConnectionPool::getActiveConnections
);

// Example: Add a custom counter
meterRegistry.counter("custom.event",
    Tags.of("type", "special")
).increment();
```

### Modify Dashboard

1. Open Grafana at http://localhost:3000
2. Go to the dashboard and click **Edit** on any panel
3. Modify the PromQL query or visualization settings
4. Click **Save dashboard**

To persist changes, export the dashboard JSON and replace `grafana/provisioning/dashboards/api-metrics-dashboard.json`

## Alerting (Optional)

Create alerts in Grafana for important metrics:

### High Error Rate Alert

1. Create a new alert rule in Grafana
2. Use query:
   ```promql
   rate(api_request_count_total{status=~"5.."}[5m]) > 0.1
   ```
3. Set threshold and notification channel

### High Latency Alert

```promql
histogram_quantile(0.95,
  sum(rate(api_request_duration_milliseconds_bucket[5m])) by (le)
) > 1000
```

## Troubleshooting

### Metrics Not Showing Up

1. Check if the application is running:
   ```bash
   curl http://localhost:8080/actuator/health
   ```

2. Verify Prometheus can scrape metrics:
   ```bash
   curl http://localhost:8080/actuator/prometheus
   ```

3. Check Prometheus targets:
   - Open http://localhost:9090/targets
   - Verify `dual-db-demo` target is UP

4. Check Prometheus logs:
   ```bash
   docker-compose logs prometheus
   ```

### Grafana Shows "No Data"

1. Verify Prometheus datasource in Grafana:
   - Go to **Configuration** → **Data Sources**
   - Click on Prometheus
   - Click **Test** button

2. Check if metrics exist in Prometheus:
   - Open Prometheus UI
   - Search for `api_products_hits_total`

### Application Not Connecting to Prometheus

1. Ensure `host.docker.internal` resolves correctly
2. For Linux, add to docker-compose.yml:
   ```yaml
   extra_hosts:
     - "host.docker.internal:172.17.0.1"
   ```

## Cleanup

Stop all services:

```bash
docker-compose down
```

Remove all data volumes:

```bash
docker-compose down -v
```

## Additional Resources

- [Micrometer Documentation](https://micrometer.io/docs)
- [Prometheus Querying Basics](https://prometheus.io/docs/prometheus/latest/querying/basics/)
- [Grafana Dashboards](https://grafana.com/docs/grafana/latest/dashboards/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
