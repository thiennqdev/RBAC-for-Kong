#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo ""
echo "=========================================="
echo "   BOOTSTRAPPING KEYCLOAK SSO..."
echo "=========================================="
bash "$SCRIPT_DIR/setup-keycloak.sh"

echo ""
echo "=========================================="
echo "   BOOTSTRAPPING KONG API GATEWAY..."
echo "=========================================="
bash "$SCRIPT_DIR/setup-kong.sh"

echo ""
echo "=========================================="
echo "   ALL SETUP COMPLETED SUCCESSFULLY! 🎉"
echo "=========================================="
echo "You can now open the Web Dashboard at: http://localhost:5173"
echo "Log in with: admin / admin123"
