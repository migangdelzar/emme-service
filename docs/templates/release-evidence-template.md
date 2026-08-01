# Release Evidence Template

Use this record for a production release or deployment promotion.

| Field | Value |
|---|---|
| Release version | <!-- e.g. 1.2.0 --> |
| Service commit | <!-- immutable SHA --> |
| Web commit | <!-- immutable SHA, if coordinated --> |
| Backend image digest | <!-- immutable digest --> |
| Web image digest | <!-- immutable digest --> |
| Migration state | <!-- compatible / applied / not required --> |
| Target environment | <!-- dev / staging / production --> |
| Approver | <!-- owner --> |
| Date | <!-- UTC --> |

## Required evidence

- [ ] Requirements and compatibility impact reviewed.
- [ ] Unit, integration, architecture, contract, and applicable E2E checks pass.
- [ ] Security/dependency/image scans pass or have an approved exception.
- [ ] Configuration and secret references validated without exposing values.
- [ ] Health, rollout, telemetry, and rollback evidence captured.
- [ ] Release notes and follow-up risks recorded.
