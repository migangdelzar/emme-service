#!/usr/bin/env bash
set -euo pipefail

POSTGRES_HOST="${POSTGRES_HOST:-postgres}"
POSTGRES_PORT="${POSTGRES_PORT:-5432}"
POSTGRES_ADMIN_DB="${POSTGRES_ADMIN_DB:-postgres}"
POSTGRES_USER="${POSTGRES_USER:-emme}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:?POSTGRES_PASSWORD is required}"
DATABASE_NAME="${DATABASE_NAME:-emme}"
DEFAULT_DATABASE_JDBC_URL="${DEFAULT_DATABASE_JDBC_URL:-jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/${DATABASE_NAME}}"
LIQUIBASE_CONTEXTS="${LIQUIBASE_CONTEXTS:-prod}"
TENANT_SEED_SLUGS="${TENANT_SEED_SLUGS:-}"
SCHEMA_VERSION="${SCHEMA_VERSION:-0.1.0}"

export PGPASSWORD="${POSTGRES_PASSWORD}"

psql_base=(
  psql
  -h "${POSTGRES_HOST}"
  -p "${POSTGRES_PORT}"
  -U "${POSTGRES_USER}"
  -v ON_ERROR_STOP=1
)

summarize_error() {
  local raw_summary
  raw_summary="$(printf '%s' "$1" | tr '\r\n' ' ' | tr -s ' ')"
  raw_summary="${raw_summary#"${raw_summary%%[![:space:]]*}"}"
  raw_summary="${raw_summary%"${raw_summary##*[![:space:]]}"}"
  if (( ${#raw_summary} > 240 )); then
    raw_summary="${raw_summary: -240}"
  fi
  printf '%.240s' "${raw_summary}"
}

echo "Waiting for PostgreSQL at ${POSTGRES_HOST}:${POSTGRES_PORT}"
until pg_isready -h "${POSTGRES_HOST}" -p "${POSTGRES_PORT}" -U "${POSTGRES_USER}" -d "${POSTGRES_ADMIN_DB}" >/dev/null 2>&1; do
  sleep 2
done

echo "Ensuring database ${DATABASE_NAME} exists"
if ! "${psql_base[@]}" -d "${POSTGRES_ADMIN_DB}" -tAc "SELECT 1 FROM pg_database WHERE datname = '${DATABASE_NAME}'" | grep -q 1; then
  createdb -h "${POSTGRES_HOST}" -p "${POSTGRES_PORT}" -U "${POSTGRES_USER}" "${DATABASE_NAME}"
fi

echo "Ensuring emme_core schema exists"
"${psql_base[@]}" -d "${DATABASE_NAME}" -c "CREATE SCHEMA IF NOT EXISTS emme_core;"

echo "Running emme-core migrations"
liquibase \
  --search-path=/liquibase/changelog/db \
  --changelog-file=emme-core/changelog.yaml \
  --url="jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/${DATABASE_NAME}" \
  --username="${POSTGRES_USER}" \
  --password="${POSTGRES_PASSWORD}" \
  --default-schema-name=emme_core \
  --liquibase-schema-name=emme_core \
  update \
  --context-filter="${LIQUIBASE_CONTEXTS}"

echo "Configuring default database connection: ${DEFAULT_DATABASE_JDBC_URL}"
default_database_jdbc_url_sql="${DEFAULT_DATABASE_JDBC_URL//\'/\'\'}"
"${psql_base[@]}" \
  -d "${DATABASE_NAME}" \
  -c "
    UPDATE emme_core.database_registry
    SET jdbc_url = '${default_database_jdbc_url_sql}'
    WHERE database_id = '00000000-0000-0000-0000-000000000000';"

if [[ -n "${TENANT_SEED_SLUGS}" ]]; then
  IFS=',' read -r -a seed_slugs <<< "${TENANT_SEED_SLUGS}"
  for slug in "${seed_slugs[@]}"; do
    [[ -z "${slug}" ]] && continue
    if [[ ! "${slug}" =~ ^[A-Za-z][A-Za-z0-9-]{0,62}$ ]]; then
      echo "Unsafe tenant slug: ${slug}" >&2
      exit 11
    fi

    schema="$(printf '%s' "${slug}" | tr '-' '_' | tr '[:upper:]' '[:lower:]')"
    "${psql_base[@]}" \
      -d "${DATABASE_NAME}" \
      -c "
        INSERT INTO emme_core.tenant_registry (slug, schema_name, database_mode, database_key, status)
        VALUES ('${slug}', '${schema}', 'SHARED', '${DATABASE_NAME}', 'PROVISIONING')
        ON CONFLICT (slug) DO UPDATE SET
          schema_name = EXCLUDED.schema_name,
          updated_at = now();"
  done
fi

mapfile -t tenant_schemas < <("${psql_base[@]}" -d "${DATABASE_NAME}" -tAc "
  SELECT schema_name
  FROM emme_core.tenant_registry
  WHERE status IN ('PROVISIONING', 'ACTIVE', 'FAILED')
  ORDER BY schema_name;")

for schema in "${tenant_schemas[@]}"; do
  [[ -z "${schema}" ]] && continue
  if [[ ! "${schema}" =~ ^[a-z][a-z0-9_]{0,62}$ ]] || [[ "${schema}" == "public" ]] || [[ "${schema}" == "emme_core" ]] || [[ "${schema}" == pg_* ]]; then
    echo "Unsafe tenant schema name: ${schema}" >&2
    exit 10
  fi

  tenant_state="$(${psql_base[@]} -d "${DATABASE_NAME}" -tAc "
    SELECT status || '|' || COALESCE(schema_version, '')
    FROM emme_core.tenant_registry
    WHERE schema_name = '${schema}';" | tr -d '[:space:]')"
  if [[ "${tenant_state}" == "ACTIVE|${SCHEMA_VERSION}" ]]; then
    echo "Tenant schema ${schema} is already active at version ${SCHEMA_VERSION}; skipping"
    continue
  fi

  echo "Migrating tenant schema ${schema}"
  "${psql_base[@]}" -d "${DATABASE_NAME}" -c "CREATE SCHEMA IF NOT EXISTS \"${schema}\";"
  if ! liquibase_output="$(
    liquibase \
      --search-path=/liquibase/changelog/db \
      --changelog-file=emme-studio/changelog.yaml \
      --url="jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/${DATABASE_NAME}?currentSchema=${schema},emme_core,public" \
      --username="${POSTGRES_USER}" \
      --password="${POSTGRES_PASSWORD}" \
      --default-schema-name="${schema}" \
      --liquibase-schema-name="${schema}" \
      update \
      --context-filter="${LIQUIBASE_CONTEXTS}" 2>&1
  )"; then
    error_summary="$(summarize_error "${liquibase_output}")"
    "${psql_base[@]}" \
      -d "${DATABASE_NAME}" \
      -v "migration_error=${error_summary}" \
      -v "tenant_schema=${schema}" <<'SQL'
UPDATE emme_core.tenant_registry
SET status = 'FAILED',
    migration_error = :'migration_error',
    updated_at = now()
WHERE schema_name = :'tenant_schema';
SQL
    printf 'Tenant migration failed for %s: %s\n' "${schema}" "${error_summary}" >&2
    exit 1
  fi
  "${psql_base[@]}" \
    -d "${DATABASE_NAME}" \
    -c "
      UPDATE emme_core.tenant_registry
      SET status = CASE WHEN status IN ('PROVISIONING', 'FAILED') THEN 'ACTIVE' ELSE status END,
          schema_version = '${SCHEMA_VERSION}',
          last_migrated_at = now(),
          migration_error = NULL,
          updated_at = now()
      WHERE schema_name = '${schema}';"
done

echo "Database migrations complete"
