# Notifications, Billing, and Payments Use Cases

| Use Case | Requirements | Preconditions | Successful Outcome |
|---|---|---|---|
| Dispatch notification | FR-066–FR-069 | Channel configuration exists; request is authorized and idempotent. | Delivery is attempted and a provider-safe outcome is recorded. |
| Review subscription | FR-070 | Owner has billing permission. | Current subscription, entitlements, limits, and billing state are displayed. |
| Enforce entitlement | FR-071 | Plan and requested capability are known. | Allowed use proceeds; disallowed use fails clearly. |
| Process payment | FR-072, FR-073 | Provider is configured; callback can be authenticated. | Charge state is applied exactly once. |
| Refund payment | FR-074 | Manager is authorized and payment is refundable. | Provider and local payment states are reconciled exactly once. |

## Boundary Rules

- Provider integrations are outbound adapters.
- Callbacks are authenticated and idempotent.
- Payment secrets never enter logs or conversation context.
