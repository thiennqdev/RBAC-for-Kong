#!/usr/bin/env bash
set -e

KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8180}"
MASTER_REALM="master"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-admin}"
TARGET_REALM="rbac-management"
CLIENT_ID="rbac-admin-frontend"
FRONTEND_URL="${FRONTEND_URL:-http://localhost:5173}"

echo "1. Waiting for Keycloak service to become ready..."
MAX_RETRIES=30
RETRIES=0
while [ $RETRIES -lt $MAX_RETRIES ]; do
    if curl -s -f -m 3 "$KEYCLOAK_URL/realms/$MASTER_REALM" > /dev/null 2>&1; then
        echo "Keycloak is online!"
        break
    fi
    RETRIES=$((RETRIES + 1))
    echo "Waiting for Keycloak to boot... ($RETRIES/$MAX_RETRIES)"
    sleep 2
done

if [ $RETRIES -ge $MAX_RETRIES ]; then
    echo "Keycloak failed to become ready in time!"
    exit 1
fi

echo "2. Getting Admin Token..."
TOKEN_RESP=$(curl -s -X POST "$KEYCLOAK_URL/realms/$MASTER_REALM/protocol/openid-connect/token" \
    -d "client_id=admin-cli" \
    -d "username=$ADMIN_USER" \
    -d "password=$ADMIN_PASS" \
    -d "grant_type=password")

ADMIN_TOKEN=$(echo "$TOKEN_RESP" | grep -o '"access_token":"[^"]*' | cut -d'"' -f4)

if [ -z "$ADMIN_TOKEN" ]; then
    echo "Failed to retrieve Keycloak admin token!"
    exit 1
fi

HEADERS=( -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" )

echo "2. Checking/Creating Realm '$TARGET_REALM'..."
STATUS_REALM=$(curl -s -o /dev/null -w "%{http_code}" "${HEADERS[@]}" "$KEYCLOAK_URL/admin/realms/$TARGET_REALM")

if [ "$STATUS_REALM" -eq 200 ]; then
    echo "Realm '$TARGET_REALM' already exists."
else
    curl -s -X POST "${HEADERS[@]}" "$KEYCLOAK_URL/admin/realms" \
        -d "{\"realm\":\"$TARGET_REALM\",\"enabled\":true,\"displayName\":\"RBAC Management Realm\"}"
    echo "Realm '$TARGET_REALM' created successfully!"
fi

echo "3. Checking/Creating Client '$CLIENT_ID'..."
CLIENT_EXIST=$(curl -s "${HEADERS[@]}" "$KEYCLOAK_URL/admin/realms/$TARGET_REALM/clients" | grep -o "\"clientId\":\"$CLIENT_ID\"" || true)

if [ -z "$CLIENT_EXIST" ]; then
    curl -s -X POST "${HEADERS[@]}" "$KEYCLOAK_URL/admin/realms/$TARGET_REALM/clients" \
        -d "{\"clientId\":\"$CLIENT_ID\",\"name\":\"RBAC Admin Frontend\",\"enabled\":true,\"publicClient\":true,\"standardFlowEnabled\":true,\"directAccessGrantsEnabled\":true,\"rootUrl\":\"$FRONTEND_URL\",\"baseUrl\":\"/\",\"redirectUris\":[\"$FRONTEND_URL/*\",\"$FRONTEND_URL\"],\"webOrigins\":[\"*\"]}"
    echo "Client '$CLIENT_ID' created successfully!"
else
    echo "Client '$CLIENT_ID' already exists."
fi

echo "4. Creating Roles..."
ROLES=("SYSTEM_ADMIN" "PROJECT_ADMIN" "MAINTAINER" "DEVELOPER" "VIEWER")
for r in "${ROLES[@]}"; do
    ROLE_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "${HEADERS[@]}" "$KEYCLOAK_URL/admin/realms/$TARGET_REALM/roles/$r")
    if [ "$ROLE_STATUS" -ne 200 ]; then
        curl -s -X POST "${HEADERS[@]}" "$KEYCLOAK_URL/admin/realms/$TARGET_REALM/roles" -d "{\"name\":\"$r\"}"
        echo "Role '$r' created."
    else
        echo "Role '$r' already exists."
    fi
done

echo "5. Creating Admin User..."
USER_ID=$(curl -s "${HEADERS[@]}" "$KEYCLOAK_URL/admin/realms/$TARGET_REALM/users?username=admin" | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4 || true)

if [ -z "$USER_ID" ]; then
    curl -s -X POST "${HEADERS[@]}" "$KEYCLOAK_URL/admin/realms/$TARGET_REALM/users" \
        -d "{\"username\":\"admin\",\"email\":\"admin@example.com\",\"firstName\":\"System\",\"lastName\":\"Admin\",\"enabled\":true,\"emailVerified\":true,\"credentials\":[{\"type\":\"password\",\"value\":\"admin123\",\"temporary\":false}]}"
    USER_ID=$(curl -s "${HEADERS[@]}" "$KEYCLOAK_URL/admin/realms/$TARGET_REALM/users?username=admin" | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)
    echo "User 'admin' created."
else
    echo "User 'admin' already exists."
fi

echo "6. Assigning SYSTEM_ADMIN role to user 'admin'..."
ROLE_JSON=$(curl -s "${HEADERS[@]}" "$KEYCLOAK_URL/admin/realms/$TARGET_REALM/roles/SYSTEM_ADMIN")
ROLE_ID=$(echo "$ROLE_JSON" | grep -o '"id":"[^"]*' | cut -d'"' -f4)

curl -s -X POST "${HEADERS[@]}" "$KEYCLOAK_URL/admin/realms/$TARGET_REALM/users/$USER_ID/role-mappings/realm" \
    -d "[{\"id\":\"$ROLE_ID\",\"name\":\"SYSTEM_ADMIN\"}]" > /dev/null

echo "Role 'SYSTEM_ADMIN' mapped to 'admin'!"
echo ">>> KEYCLOAK SETUP COMPLETED! <<<"
