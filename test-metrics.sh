#!/bin/bash

# Script to generate API traffic for testing metrics

BASE_URL="http://localhost:8080"
COLORS=(
  '\033[0;32m' # Green
  '\033[0;33m' # Yellow
  '\033[0;34m' # Blue
  '\033[0;35m' # Magenta
  '\033[0;36m' # Cyan
)
NC='\033[0m' # No Color

echo -e "${COLORS[2]}================================================${NC}"
echo -e "${COLORS[2]}  API Metrics Test - Traffic Generator${NC}"
echo -e "${COLORS[2]}================================================${NC}"
echo ""

# Function to print colored output
print_status() {
  local color=$1
  local message=$2
  echo -e "${COLORS[$color]}${message}${NC}"
}

# Check if application is running
print_status 3 "Checking if application is running..."
if ! curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/health" | grep -q "200"; then
  print_status 1 "ERROR: Application is not running at $BASE_URL"
  print_status 1 "Please start the application first with: mvn spring-boot:run"
  exit 1
fi
print_status 0 "Application is running!"
echo ""

# Function to hit GET /api/products/all
test_get_products() {
  local count=$1
  print_status 4 "Generating $count GET requests to /api/products/all..."

  for i in $(seq 1 "$count"); do
    response=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/products/all")
    if [ "$response" == "200" ]; then
      echo -n "."
    else
      echo -n "x"
    fi
    sleep 0.05
  done
  echo ""
  print_status 0 "Completed $count GET requests"
  echo ""
}

# Function to create random products
test_post_products() {
  local count=$1
  print_status 4 "Creating $count random products via POST /api/products/sync..."

  for i in $(seq 1 "$count"); do
    random_id=$((RANDOM % 10000))
    response=$(curl -s -o /dev/null -w "%{http_code}" \
      -X POST "$BASE_URL/api/products/sync" \
      -H "Content-Type: application/json" \
      -d "{
        \"name\": \"Test Product $random_id\",
        \"description\": \"Auto-generated product for metrics testing\",
        \"price\": $((RANDOM % 1000 + 1))
      }")

    if [ "$response" == "201" ]; then
      echo -n "+"
    else
      echo -n "x"
    fi
    sleep 0.1
  done
  echo ""
  print_status 0 "Completed $count POST requests"
  echo ""
}

# Function to generate mixed traffic
test_mixed_traffic() {
  local duration=$1
  print_status 4 "Generating mixed traffic for $duration seconds..."

  local end_time=$(($(date +%s) + duration))
  local request_count=0

  while [ $(date +%s) -lt $end_time ]; do
    # Random choice between GET and POST
    if [ $((RANDOM % 3)) -eq 0 ]; then
      # POST request (33% chance)
      random_id=$((RANDOM % 10000))
      curl -s -o /dev/null -X POST "$BASE_URL/api/products/sync" \
        -H "Content-Type: application/json" \
        -d "{
          \"name\": \"Product $random_id\",
          \"description\": \"Test product\",
          \"price\": $((RANDOM % 500 + 1))
        }" &
      echo -n "+"
    else
      # GET request (67% chance)
      curl -s -o /dev/null "$BASE_URL/api/products/all" &
      echo -n "."
    fi

    request_count=$((request_count + 1))
    sleep 0.1
  done

  wait
  echo ""
  print_status 0 "Completed $request_count mixed requests in $duration seconds"
  echo ""
}

# Main menu
while true; do
  print_status 2 "Select test scenario:"
  echo "  1) Quick test - 50 GET requests"
  echo "  2) Moderate test - 200 GET requests"
  echo "  3) Load test - 1000 GET requests"
  echo "  4) Create products - 20 POST requests"
  echo "  5) Mixed traffic - 30 seconds"
  echo "  6) Mixed traffic - 60 seconds"
  echo "  7) Continuous load (Ctrl+C to stop)"
  echo "  8) View metrics"
  echo "  9) Open Grafana dashboard"
  echo "  0) Exit"
  echo ""
  read -p "Enter your choice: " choice
  echo ""

  case $choice in
    1)
      test_get_products 50
      ;;
    2)
      test_get_products 200
      ;;
    3)
      test_get_products 1000
      ;;
    4)
      test_post_products 20
      ;;
    5)
      test_mixed_traffic 30
      ;;
    6)
      test_mixed_traffic 60
      ;;
    7)
      print_status 4 "Starting continuous load test... Press Ctrl+C to stop"
      while true; do
        test_mixed_traffic 10
      done
      ;;
    8)
      print_status 3 "Opening Prometheus metrics endpoint..."
      if command -v open &> /dev/null; then
        open "$BASE_URL/actuator/prometheus"
      elif command -v xdg-open &> /dev/null; then
        xdg-open "$BASE_URL/actuator/prometheus"
      else
        print_status 1 "Please open manually: $BASE_URL/actuator/prometheus"
      fi
      echo ""
      ;;
    9)
      print_status 3 "Opening Grafana dashboard..."
      if command -v open &> /dev/null; then
        open "http://localhost:3000/d/dual-db-api-metrics/dual-db-demo-api-metrics"
      elif command -v xdg-open &> /dev/null; then
        xdg-open "http://localhost:3000/d/dual-db-api-metrics/dual-db-demo-api-metrics"
      else
        print_status 1 "Please open manually: http://localhost:3000"
      fi
      echo ""
      ;;
    0)
      print_status 0 "Goodbye!"
      exit 0
      ;;
    *)
      print_status 1 "Invalid choice. Please try again."
      echo ""
      ;;
  esac

  # Show quick stats after each test
  print_status 3 "Current metrics available at:"
  echo "  - Prometheus: http://localhost:9090"
  echo "  - Grafana: http://localhost:3000"
  echo "  - Actuator Metrics: $BASE_URL/actuator/prometheus"
  echo ""
  echo -e "${COLORS[2]}------------------------------------------------${NC}"
  echo ""
done
