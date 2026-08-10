#!/usr/bin/env bash
set -euo pipefail
# Cleanup E2E tenant data from DB and Keycloak
# Keeps emme-core (platform admin) tenant

echo "Cleaning E2E data..."

ADMIN_TOKEN=$(curl -s -X POST 'http://localhost:18080/realms/master/protocol/openid-connect/token' \
  -d 'grant_type=password&client_id=admin-cli&username=admin&password=e2e-admin-password' | \
  python3 -c "import sys,json; print(json.load(sys.stdin).get('access_token',''))")

if [ -z "$ADMIN_TOKEN" ]; then
  echo "ERROR: Could not get Keycloak admin token"
  exit 1
fi

docker exec compose-postgres-1 psql -U emme -d emme -c \
  "DELETE FROM emme_core.membership WHERE tenant_id IN (SELECT id FROM emme_core.tenant WHERE slug != 'emme-core');
   DELETE FROM emme_core.tenant_registry WHERE slug != 'emme-core';
   DELETE FROM emme_core.tenant WHERE slug != 'emme-core';
   DROP SCHEMA IF EXISTS e2e_studio CASCADE;
   DROP SCHEMA IF EXISTS e2e_salon CASCADE;"

for realm in emme-e2e-studio emme-e2e-salon emme-test-salon; do
  curl -s -X DELETE "http://localhost:18080/admin/realms/$realm" -H "Authorization: Bearer $ADMIN_TOKEN" > /dev/null 2>&1
done

echo "✓ E2E data cleaned (emme-core preserved)"
