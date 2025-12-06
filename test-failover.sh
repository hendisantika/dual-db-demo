#!/bin/bash

# Failover Test Script for Dual DB Demo
# This script helps test database failover scenarios

set -e

BASE_URL="http://localhost:8080"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

wait_for_app() {
    log_info "Waiting for application to be ready..."
    for i in {1..30}; do
        if curl -s "${BASE_URL}/actuator/health" | grep -q "UP"; then
            log_info "Application is ready!"
            return 0
        fi
        sleep 2
    done
    log_error "Application failed to start"
    return 1
}

create_mysql_product() {
    local name="$1"
    local response=$(curl -s -X POST "${BASE_URL}/api/mysql/products" \
        -H "Content-Type: application/json" \
        -d "{\"name\":\"${name}\",\"description\":\"Test\",\"price\":99.99,\"quantity\":10}")
    echo "$response"
}

create_postgres_product() {
    local name="$1"
    local response=$(curl -s -X POST "${BASE_URL}/api/postgres/products" \
        -H "Content-Type: application/json" \
        -d "{\"name\":\"${name}\",\"description\":\"Test\",\"price\":99.99,\"quantity\":10}")
    echo "$response"
}

get_mysql_products() {
    curl -s "${BASE_URL}/api/mysql/products"
}

get_postgres_products() {
    curl -s "${BASE_URL}/api/postgres/products"
}

get_all_products() {
    curl -s "${BASE_URL}/api/products/all"
}

sync_product() {
    local name="$1"
    curl -s -X POST "${BASE_URL}/api/products/sync" \
        -H "Content-Type: application/json" \
        -d "{\"name\":\"${name}\",\"description\":\"Synced\",\"price\":49.99,\"quantity\":5}"
}

test_basic_crud() {
    log_info "Testing basic CRUD operations..."

    # Create products
    log_info "Creating MySQL product..."
    create_mysql_product "Test MySQL Product $(date +%s)"

    log_info "Creating PostgreSQL product..."
    create_postgres_product "Test PostgreSQL Product $(date +%s)"

    # Read products
    log_info "Reading MySQL products..."
    get_mysql_products
    echo ""

    log_info "Reading PostgreSQL products..."
    get_postgres_products
    echo ""

    log_info "Reading all products from both databases..."
    get_all_products
    echo ""

    log_info "Basic CRUD test completed successfully!"
}

test_mysql_failover() {
    log_info "=== Testing MySQL Failover ==="

    log_info "Step 1: Create product with primary host active"
    create_mysql_product "Before Failover $(date +%s)"
    echo ""

    log_warn "Step 2: Stopping MySQL primary (port 3308)..."
    docker compose stop mysql-primary

    sleep 3

    log_info "Step 3: Attempting to create product (should failover to secondary)..."
    create_mysql_product "During Failover $(date +%s)"
    echo ""

    log_info "Step 4: Verifying data is accessible..."
    get_mysql_products
    echo ""

    log_info "Step 5: Restarting MySQL primary..."
    docker compose start mysql-primary

    sleep 5

    log_info "Step 6: Creating product after primary is back..."
    create_mysql_product "After Failover $(date +%s)"
    echo ""

    log_info "MySQL failover test completed!"
}

test_postgres_failover() {
    log_info "=== Testing PostgreSQL Failover ==="

    log_info "Step 1: Create product with primary host active"
    create_postgres_product "Before Failover $(date +%s)"
    echo ""

    log_warn "Step 2: Stopping PostgreSQL primary (port 5433)..."
    docker compose stop postgres-primary

    sleep 3

    log_info "Step 3: Attempting to create product (should failover to secondary)..."
    create_postgres_product "During Failover $(date +%s)"
    echo ""

    log_info "Step 4: Verifying data is accessible..."
    get_postgres_products
    echo ""

    log_info "Step 5: Restarting PostgreSQL primary..."
    docker compose start postgres-primary

    sleep 5

    log_info "Step 6: Creating product after primary is back..."
    create_postgres_product "After Failover $(date +%s)"
    echo ""

    log_info "PostgreSQL failover test completed!"
}

test_dual_sync() {
    log_info "=== Testing Dual Database Sync ==="

    log_info "Syncing product to both databases..."
    sync_product "Synced Product $(date +%s)"
    echo ""

    log_info "Checking MySQL..."
    get_mysql_products
    echo ""

    log_info "Checking PostgreSQL..."
    get_postgres_products
    echo ""

    log_info "Dual sync test completed!"
}

show_usage() {
    echo "Usage: $0 <command>"
    echo ""
    echo "Commands:"
    echo "  start       - Start all database containers and the application"
    echo "  stop        - Stop all containers"
    echo "  basic       - Run basic CRUD tests"
    echo "  mysql       - Test MySQL failover scenario"
    echo "  postgres    - Test PostgreSQL failover scenario"
    echo "  sync        - Test dual database sync"
    echo "  all         - Run all tests"
    echo "  status      - Show container status"
    echo ""
}

case "$1" in
    start)
        log_info "Starting all database containers..."
        docker compose up -d
        sleep 10
        log_info "Starting the application..."
        log_info "Run: ./mvnw spring-boot:run"
        ;;
    stop)
        log_info "Stopping all containers..."
        docker compose down
        ;;
    basic)
        wait_for_app && test_basic_crud
        ;;
    mysql)
        wait_for_app && test_mysql_failover
        ;;
    postgres)
        wait_for_app && test_postgres_failover
        ;;
    sync)
        wait_for_app && test_dual_sync
        ;;
    all)
        wait_for_app
        test_basic_crud
        echo ""
        test_dual_sync
        echo ""
        test_mysql_failover
        echo ""
        test_postgres_failover
        ;;
    status)
        docker compose ps
        ;;
    *)
        show_usage
        exit 1
        ;;
esac
