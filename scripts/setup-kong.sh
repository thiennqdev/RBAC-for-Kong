#!/usr/bin/env bash
set -e

KONG_ADMIN_URL="${KONG_ADMIN_URL:-http://localhost:8001}"
BACKEND_URL="${BACKEND_URL:-http://backend:8080}"
SERVICE_NAME="rbac-backend"
ROUTE_NAME="rbac-backend-route"
ROUTE_PATH="/api/rbac"

echo "1. Waiting for Kong Admin API to become ready..."
MAX_RETRIES=30
RETRIES=0
while [ $RETRIES -lt $MAX_RETRIES ]; do
    if curl -s -f -m 3 "$KONG_ADMIN_URL/status" > /dev/null 2>&1; then
        echo "Kong Admin API is online!"
        break
    fi
    RETRIES=$((RETRIES + 1))
    echo "Waiting for Kong migrations and service to boot... ($RETRIES/$MAX_RETRIES)"
    sleep 2
done

if [ $RETRIES -ge $MAX_RETRIES ]; then
    echo "Kong Admin API failed to become ready in time!"
    exit 1
fi

echo "2. Configuring Service '$SERVICE_NAME'..."
SERVICE_ID=$(curl -s "$KONG_ADMIN_URL/services/$SERVICE_NAME" | grep -o '"id":"[^"]*' | cut -d'"' -f4 || true)

if [ -z "$SERVICE_ID" ]; then
    RESPONSE=$(curl -s -X POST "$KONG_ADMIN_URL/services" \
        -H "Content-Type: application/json" \
        -d "{\"name\":\"$SERVICE_NAME\",\"url\":\"$BACKEND_URL\"}")
    SERVICE_ID=$(echo "$RESPONSE" | grep -o '"id":"[^"]*' | cut -d'"' -f4)
    echo "Service '$SERVICE_NAME' created (ID: $SERVICE_ID) pointing to $BACKEND_URL"
else
    echo "Service '$SERVICE_NAME' already exists (ID: $SERVICE_ID)."
fi

echo "3. Configuring Route '$ROUTE_NAME'..."
ROUTE_CHECK=$(curl -s "$KONG_ADMIN_URL/services/$SERVICE_ID/routes" | grep -o "\"name\":\"$ROUTE_NAME\"" || true)

if [ -z "$ROUTE_CHECK" ]; then
    curl -s -X POST "$KONG_ADMIN_URL/services/$SERVICE_ID/routes" \
        -H "Content-Type: application/json" \
        -d "{\"name\":\"$ROUTE_NAME\",\"paths\":[\"$ROUTE_PATH\"],\"strip_path\":true}" > /dev/null
    echo "Route '$ROUTE_NAME' created with path '$ROUTE_PATH' (strip_path: true)"
else
    echo "Route '$ROUTE_NAME' already exists."
fi

echo ">>> KONG SETUP COMPLETED! <<<"
