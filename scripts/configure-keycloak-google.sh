#!/usr/bin/env bash
set -euo pipefail

: "${GOOGLE_SOCIAL_CLIENT_ID:?Set GOOGLE_SOCIAL_CLIENT_ID to the Google OAuth web client ID}"
: "${GOOGLE_SOCIAL_CLIENT_SECRET:?Set GOOGLE_SOCIAL_CLIENT_SECRET to the Google OAuth web client secret}"

keycloak_url="${KEYCLOAK_URL:-http://localhost:18080}"
keycloak_realm="${KEYCLOAK_CUSTOMER_REALM:-emme-customers}"
admin_realm="${KEYCLOAK_ADMIN_REALM:-master}"
admin_username="${KEYCLOAK_ADMIN_USERNAME:-admin}"
admin_password="${KEYCLOAK_ADMIN_PASSWORD:-${EMME_E2E_KEYCLOAK_ADMIN_PASSWORD:-e2e-admin-password}}"

token_response="$(curl --fail-with-body --silent --show-error \
  --data-urlencode "grant_type=password" \
  --data-urlencode "client_id=admin-cli" \
  --data-urlencode "username=${admin_username}" \
  --data-urlencode "password=${admin_password}" \
  "${keycloak_url}/realms/${admin_realm}/protocol/openid-connect/token")"

admin_token="$(printf '%s' "${token_response}" | jq --raw-output '.access_token')"
if [[ -z "${admin_token}" || "${admin_token}" == "null" ]]; then
  echo "Keycloak did not return an administrative access token." >&2
  exit 1
fi

provider_payload="$(jq -n \
  --arg client_id "${GOOGLE_SOCIAL_CLIENT_ID}" \
  --arg client_secret "${GOOGLE_SOCIAL_CLIENT_SECRET}" \
  '{alias: "google", displayName: "Google", providerId: "google", enabled: true,
    trustEmail: true, storeToken: false, firstBrokerLoginFlowAlias: "first broker login",
    config: {clientId: $client_id, clientSecret: $client_secret,
      defaultScope: "openid profile email"}}')"

provider_url="${keycloak_url}/admin/realms/${keycloak_realm}/identity-provider/instances/google"
provider_status="$(curl --silent --show-error --output /dev/null --write-out '%{http_code}' \
  --header "Authorization: Bearer ${admin_token}" "${provider_url}")"

if [[ "${provider_status}" == "200" ]]; then
  curl --fail-with-body --silent --show-error --request PUT \
    --header "Authorization: Bearer ${admin_token}" \
    --header 'Content-Type: application/json' \
    --data "${provider_payload}" "${provider_url}" >/dev/null
  action="updated"
else
  curl --fail-with-body --silent --show-error --request POST \
    --header "Authorization: Bearer ${admin_token}" \
    --header 'Content-Type: application/json' \
    --data "${provider_payload}" \
    "${keycloak_url}/admin/realms/${keycloak_realm}/identity-provider/instances" >/dev/null
  action="created"
fi

echo "Google identity provider ${action} in Keycloak realm ${keycloak_realm}."
