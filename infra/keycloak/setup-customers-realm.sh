#!/bin/bash
# Creates the emme-customers realm for social login.
# Requires Keycloak running on localhost:18080 with admin/admin credentials.

set -e

KC_URL="${KEYCLOAK_URL:-http://localhost:18080}"
echo "=== Setting up emme-customers realm ==="

# Get admin token
ADMIN_TOKEN=$(curl -s -X POST "$KC_URL/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=admin-cli&username=admin&password=admin" \
  | python3 -c "import json,sys; print(json.load(sys.stdin)['access_token'])")

# Create realm
curl -s -X POST "$KC_URL/admin/realms" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"realm":"emme-customers","enabled":true,"displayName":"Emme Customers"}' \
  -w "  Realm: HTTP %{http_code}\n" -o /dev/null

# Create client
curl -s -X POST "$KC_URL/admin/realms/emme-customers/clients" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId":"emme-customer-app",
    "publicClient":true,
    "directAccessGrantsEnabled":true,
    "standardFlowEnabled":true,
    "redirectUris":["http://localhost:3000/*","http://localhost:8080/*"]
  }' \
  -w "  Client: HTTP %{http_code}\n" -o /dev/null

# Create customer role
curl -s -X POST "$KC_URL/admin/realms/emme-customers/roles" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"customer"}' \
  -w "  Role: HTTP %{http_code}\n" -o /dev/null

echo ""
echo "=== Setup complete ==="
echo "Next: Configure Google/Facebook/Twitter Identity Providers:"
echo "  Keycloak Admin UI → emme-customers → Identity Providers"
echo "  Add provider → Google/Facebook/Twitter (OIDC)"
echo "  Enter Client ID and Client Secret from developer console"
