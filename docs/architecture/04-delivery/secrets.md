# Secrets and Configuration Boundary

> **Scope:** This document is the canonical secret inventory for `emme-service`.
> It describes where values belong; it never stores secret values.

## Operating rule

GitHub Actions secrets are for CI/CD execution only. Local development should
load secrets from a developer-managed secret store such as Bitwarden Secrets
Manager, 1Password, or macOS Keychain. Production workloads should use the
deployment platform's secret manager and workload identity/OIDC where
available. Do not commit `.env` files, credentials, API keys, or generated
reports containing environment metadata.

GitHub provides `GITHUB_TOKEN` automatically. It must not be created manually.

## Canonical environments

Build and deployment configuration uses exactly these environment names:

`local`, `dev`, `regression`, `staging`, and `production`.

`e2e` and `prod` are not valid environment names. E2E execution belongs to the
`regression` environment, while `production` is always written in full.

## GitHub Actions secrets

The following are the only manually configured secrets currently referenced by
the service workflows:

| Secret | Scope | Workflow | Required when | Notes |
|---|---|---|---|---|
| `NVD_API_KEY` | Repository secret | `security-scan.yml` | Manual `Security Scan` with `require_nvd=true` | Free NVD API key; not required by the normal fail-open dependency lane |
| `E2E_ACCESS_TOKEN` | `regression` environment secret | `ci-backend.yml` | Manual dispatch with `run_e2e=true` | Token for the explicitly supplied disposable/staging API URL |
| `SMOKE_TOKEN` | `production` environment secret | `ci-doctor-smoke.yml` | Successful `CI Backend` workflow on `main` | Read-only smoke identity; never use an administrator token |

The repository should use `regression` and `production` environments. Protect
`production` with required reviewers and branch/tag restrictions before storing
the smoke credential there.

## Runtime secret inventory

These values belong in deployment secrets, not in source control or pull-request
jobs. Only configure a provider's values when that provider is enabled.

| Capability | Secret names currently consumed |
|---|---|
| Database | `DB_PASSWORD`, `DATABASE_PASSWORD`, `POSTGRES_PASSWORD` |
| Identity | `EMME_KEYCLOAK_ADMIN_PASSWORD`, `EMME_KEYCLOAK_PROVISIONING_INITIAL_ADMIN_PASSWORD` |
| AI | `GROQ_API_KEY` |
| WhatsApp | `WHATSAPP_VERIFY_TOKEN`, `WHATSAPP_APP_SECRET`, `WHATSAPP_ACCESS_TOKEN` |
| Email | `SMTP_PASSWORD`, `SENDGRID_API_KEY`, `AWS_SECRET_ACCESS_KEY` |
| Push notifications | `TWILIO_AUTH_TOKEN`, `MESSAGEBIRD_API_KEY`, `VONAGE_API_SECRET`, `FCM_SERVICE_ACCOUNT_BASE64`, `APNS_PRIVATE_KEY_BASE64` |
| Google integration | `GOOGLE_SA_JSON_BASE64`, `GOOGLE_OAUTH_CLIENT_SECRET`, `GOOGLE_TOKEN_ENCRYPTION_KEY` |
| Payments | `MP_ACCESS_TOKEN`, `MP_WEBHOOK_SECRET`, `PAYPAL_CLIENT_SECRET`, `PAYPAL_WEBHOOK_ID`, `CONEKTA_PRIVATE_KEY`, `CONEKTA_WEBHOOK_SECRET`, `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET` |
| Infrastructure | `GRAFANA_PASSWORD`, `HCLOUD_TOKEN` when Terraform is used |
| Kafka | `KAFKA_SASL_JAAS_CONFIG` when production uses `SASL_SSL` |

Identifiers such as client IDs, phone IDs, project IDs, bundle IDs, and public
keys are configuration, not automatically secrets. They still require review
before being exposed to a browser or logged.

## Required hardening follow-ups

- Normalize `DB_PASSWORD`/`DATABASE_PASSWORD` at the deployment boundary and
  document the translation explicitly until the application configuration is
  unified.
- Do not allow the production default
  `GOOGLE_TOKEN_ENCRYPTION_KEY=change-me-in-production`; fail startup when a
  production value is absent.
- If production Kafka uses `SASL_SSL`, provide `KAFKA_SASL_JAAS_CONFIG` through
  the deployment secret store. `KAFKA_BOOTSTRAP_SERVERS` and
  `KAFKA_SECURITY_PROTOCOL` alone do not authenticate a secured broker.
- Keep local Compose defaults confined to local/E2E profiles and never reuse
  them for a production deployment.

## Safe setup commands

Run these only after creating the corresponding GitHub environments and after
rotating any credential that has been exposed outside its secret manager:

```bash
gh secret set NVD_API_KEY --repo migangdelzar/emme-service
gh secret set E2E_ACCESS_TOKEN --repo migangdelzar/emme-service --env regression
gh secret set SMOKE_TOKEN --repo migangdelzar/emme-service --env production
```

Values are entered interactively or piped from a secret manager; never place
them in shell history, Markdown, workflow files, or command arguments copied
into tickets.

## Verification checklist

- [ ] `gh secret list --repo migangdelzar/emme-service` contains no unexpected values.
- [ ] `regression` and `production` environments have protection rules.
- [ ] Pull-request jobs cannot read production secrets.
- [ ] Manual NVD execution with `require_nvd=true` fails closed when the key is absent.
- [ ] Provider credentials are tested only in disposable or approved environments.
- [ ] Logs, traces, Playwright reports, and uploaded artifacts are reviewed for secret leakage.
