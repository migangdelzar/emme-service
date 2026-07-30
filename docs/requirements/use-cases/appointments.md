# Appointment Use Cases

| Use Case | Requirements | Preconditions | Successful Outcome |
|---|---|---|---|
| Find available slot | FR-030 | Tenant, service duration, working hours, and artist constraints are known. | Only valid, non-overlapping slots are returned. |
| Create appointment | FR-031, FR-032 | Customer, service, and slot are valid; idempotency key and lock are acquired. | Exactly one appointment is committed and a booking event is published. |
| Reschedule appointment | FR-033 | Appointment is eligible; replacement slot is valid and locked. | Appointment moves once and the old slot becomes available. |
| Cancel appointment | FR-034 | Appointment exists and cancellation policy permits the actor. | Appointment is cancelled once and follow-up events are published. |
| Progress appointment | FR-035 | Requested transition is valid for the current status. | Status changes and is recorded for reporting. |
| Review schedule and detail | FR-029, FR-036, FR-037 | User has schedule permission. | Schedule, details, filters, and approved export are available. |

## Boundary Rules

- PostgreSQL owns appointment state; Redis coordinates locks and idempotency only.
- Availability is never inferred from RAG or an LLM.
- Consequential customer-chat actions require explicit confirmation.
