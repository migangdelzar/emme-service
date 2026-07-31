# Security and Privacy

Security is a boundary property and a release gate. Apply least privilege,
defense in depth, secure defaults, and explicit denial.

## Mandatory controls

- Authenticate every protected operation.
- Authorize the specific action and resource on the server.
- Validate syntax at transport boundaries and business invariants in the domain.
- Use parameterized queries and safe command APIs.
- Allowlist redirects, origins, sort fields, and external URLs.
- Return stable error codes without stack traces or internal topology.
- Keep secrets, tokens, credentials, health answers, and payment data out of
  source, logs, metrics, traces, URLs, fixtures, and browser storage.
- Load secrets from environment configuration or an approved secret manager.
- Define retention, deletion, backup, and restoration for durable data.
- Lock dependencies and scan source, dependencies, images, and SBOMs.
- Run containers as non-root with minimal runtime contents.
- Pin CI actions and grant only the permissions a job requires.

## Logging and observability

- Correlation IDs MUST contain no business data.
- Redact authorization headers, cookies, tokens, credentials, and free-form user
  input.
- Audit security-sensitive changes with actor, action, outcome, and time without
  storing secret material.
- Alert on authentication abuse, authorization denials, and unusual dependency
  failures.

## Verification

- [ ] Positive and negative authorization tests exist.
- [ ] Validation and injection cases are covered.
- [ ] Logs and errors were inspected for sensitive data.
- [ ] Dependency, secret, and image scans pass.
- [ ] Changed boundaries have an updated threat model or recorded rationale.

See the boundary-specific controls in [API](architecture/01-backend/api.md),
[integration](architecture/03-integration/frontend-backend.md), and
[containers](architecture/04-delivery/container.md).
