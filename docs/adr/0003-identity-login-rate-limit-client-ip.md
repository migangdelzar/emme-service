# ADR-0003: Trust forwarded client IPs only from configured proxies

## Status

Accepted

## Date

2026-08-01

## Context

Identity rate-limits password login attempts by client IP. The previous filter
always used the first value in `X-Forwarded-For`, which allowed a direct caller
to rotate that header and evade the limiter. The service also needs to work
behind a reverse proxy without rate-limiting every end user under the proxy's
address.

## Decision drivers

- Direct deployments must not trust client-supplied forwarding headers.
- Reverse-proxy deployments must be able to preserve the actual client address.
- Rate-limit settings must be typed and externally configurable.
- The default must be secure without requiring local development configuration.
- Existing login routes, response status, and default limits must remain stable.

## Considered options

### Always trust `X-Forwarded-For`

Simple and compatible with a proxy, but unsafe when the service is reachable
directly or the proxy does not overwrite incoming forwarding headers.

### Never trust forwarded headers

Safe against spoofing, but all users behind a shared reverse proxy would share
one limiter bucket.

### Trust forwarded headers only from configured proxy networks

The service checks the socket peer address against configured CIDR or IP
networks. It uses the first forwarded address only when that peer is trusted.
This provides secure direct-deployment defaults and explicit proxy operation.

## Decision

Identity uses the configured-proxy model.

Configuration is owned by `IdentityRateLimitProperties`:

```yaml
app:
  identity:
    login-rate-limit:
      max-attempts: 5
      window-ms: 60000
      trusted-proxies: []
```

An empty `trusted-proxies` list means that `X-Forwarded-For` is ignored and
`HttpServletRequest.getRemoteAddr()` is the rate-limit key. When the immediate
peer matches a configured network, the first `X-Forwarded-For` value may be
used as the client key.

## Consequences

### Positive

- Header spoofing cannot bypass the limiter in the default configuration.
- Proxy trust becomes explicit, reviewable, and environment-specific.
- Spring configuration is typed instead of field-level `@Value` injection.
- Unit tests cover both direct and trusted-proxy behavior.

### Negative

- Operators must configure proxy CIDRs when deploying behind a reverse proxy.
- The current limiter remains in-memory and therefore is not shared across
  service replicas; distributed rate limiting is a separate capability decision.

### Risks and mitigations

| Risk | Mitigation |
|---|---|
| An overly broad trusted network accepts spoofed headers | Configure only the actual ingress/proxy CIDRs and review them as deployment configuration |
| Proxy topology changes without configuration updates | Add deployment smoke checks and monitor rate-limit key distribution |
| Multiple replicas dilute enforcement | Move the limiter state to a shared store in a future approved slice |

## Verification

- `LoginRateLimitFilterTest` proves untrusted forwarded-header rotation is
  blocked and trusted proxy forwarding remains functional.
- `IdentityRateLimitPropertiesTest` proves typed defaults and defensive list
  copying.
- Full Identity, Modulith, CI, formatting, and boot-JAR gates remain required
  before merge.
