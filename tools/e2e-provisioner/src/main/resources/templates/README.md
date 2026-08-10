# E2E Provisioner Templates

JSON template files that define the E2E environment. The provisioner loads these, resolves `${ENV_VAR}` placeholders, and applies them idempotently.

## Directory

```
templates/
├── keycloak/
│   ├── realm.json     — Keycloak realm, clients, roles, scopes, protocol mappers, user profile
│   └── users.json     — User definitions with credentials, roles, and tenant attributes
└── database/
    └── seed.json      — Tenant, subscription, permissions, and sample business data
```

## Placeholders

Template files support `${ENV_VAR}` syntax:
- `${E2E_ADMIN_USERNAME}` — Tenant admin username (default: `admin`)
- `${E2E_ADMIN_PASSWORD}` — Tenant admin password
- `${E2E_OWNER_USERNAME}` — Tenant owner username (default: `owner`)
- `${E2E_OWNER_PASSWORD}` — Tenant owner password
- `${E2E_TENANT_SLUG}` — Tenant URL slug (default: `e2e-studio`)
- `${E2E_TENANT_NAME}` — Tenant display name (default: `E2E Studio`)
- `${E2E_TENANT_ID}` — Tenant UUID (resolved at runtime)
- `${E2E_WEB_ORIGIN}` — Web app origin for CORS (default: `http://localhost:3000`)

The provisioner writes one local-only Playwright artifact per tenant when
`E2E_TENANT_ADMIN_PASSWORD` and `E2E_TENANT_OWNER_PASSWORD` are available.
Set `E2E_PROVISIONER_AUTH_DIR` to choose the output directory (default:
`build/provisioned-auth`). The files contain the admin/owner credentials and
empty storage-state placeholders; Playwright fills the storage state during
the real-provider login setup. Do not commit or publish these files.

## Usage

```bash
KEYCLOAK_ADMIN_PASSWORD=e2e-admin-password \
E2E_OWNER_USERNAME=e2e-owner \
E2E_OWNER_PASSWORD=e2e-owner-password \
./gradlew :tools:e2e-provisioner:run
```

## Design

Templates separate data from code, enabling:
- **Reviewable**: Standard JSON, readable by any tool
- **Reusable**: Same templates for local dev, CI, regression
- **Versioned**: Changes tracked in git alongside code
- **Extensible**: Add new users, services, or customers without recompiling
