#!/bin/bash
# Migrates users from monolithic "emme" realm to per-tenant realms.
# Run ONCE after deploying the new code. Requires Keycloak running locally.

set -e

KC_URL="${KEYCLOAK_URL:-http://localhost:18080}"
ADMIN_TOKEN=$(curl -s -X POST "$KC_URL/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=admin-cli&username=admin&password=admin" \
  | python3 -c "import json,sys; print(json.load(sys.stdin)['access_token'])")

echo "=== Migrating users from 'emme' realm to per-tenant realms ==="

# Create per-tenant realms
for realm_info in "emme-demo-salon:Demo Salon" "emme-studio-a:Studio A"; do
  REALM="${realm_info%%:*}"
  NAME="${realm_info##*:}"
  echo "Creating realm: $REALM ($NAME)"
  curl -s -X POST "$KC_URL/admin/realms" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"realm\":\"$REALM\",\"enabled\":true,\"displayName\":\"$NAME\"}" \
    -w "  HTTP %{http_code}\n" -o /dev/null
done

echo ""
echo "=== Realms created. Now update the database: ==="
echo ""
echo "Run these SQL commands:"
echo "  UPDATE emme_core.tenant SET keycloak_realm = 'emme-demo-salon' WHERE slug = 'demo-salon';"
echo "  UPDATE emme_core.tenant SET keycloak_realm = 'emme-studio-a' WHERE slug = 'studio-a';"
echo ""
echo "Then restart the backend. Tenant provisioning will seed roles and users."
echo "Or manually create each tenant to trigger KeycloakRealmProvisioner."
