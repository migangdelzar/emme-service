# Use Case: Sync Personal Calendar

## Overview

**Use Case ID:** UC-026
**Use Case Name:** Sync Personal Calendar
**Primary Actor:** Customer
**Goal:** Sync confirmed appointments to my personal calendar so that I can see them alongside my other events.
**Status:** Draft

## Preconditions

- Customer has an active appointment in CONFIRMED status.
- The customer's channel identity is linked to a customer profile.
- Calendar sync for the tenant is configured and active.

## Main Success Scenario

1. After a booking is confirmed, system offers to sync the appointment to the customer's personal calendar.
2. Customer accepts the sync offer.
3. System requests calendar sync through the tenant's configured calendar provider.
4. System creates an external calendar event linked to the appointment.
5. System records the calendar event link with SYNCED status.
6. Customer later cancels the appointment.
7. System detects the cancellation and removes the external calendar event.
8. System updates the calendar event link status to DELETED, achieving personal calendar synchronization.

## Alternative Flows

### A1: Customer Declines Sync

**Trigger:** Customer declines the sync offer (step 2)
**Flow:**

1. System records the preference and does not create a calendar event.
2. Use case ends.

### A2: Sync Fails

**Trigger:** Calendar provider returns an error (step 4)
**Flow:**

1. System records the event link with FAILED status.
2. System informs the customer that sync was not completed.
3. Customer may retry through the studio staff.
4. Use case ends.

### A3: Appointment Rescheduled

**Trigger:** Appointment is rescheduled after sync (step 5)
**Flow:**

1. System updates the external calendar event with the new time.
2. System updates the calendar event link status to reflect the change.
3. Customer's calendar reflects the updated appointment.
4. Use case ends.

## Postconditions

### Success Postconditions

- The appointment appears in the customer's personal calendar.
- Cancelled appointments are removed from the customer's calendar.

### Failure Postconditions

- No calendar event is created; the customer can request sync through studio staff.

## Business Rules

### BR-035: Sync Only Confirmed Appointments

Only appointments in CONFIRMED or IN_PROGRESS status are eligible for calendar sync.

### BR-036: Customer Consent Required

Calendar sync for customer appointments requires explicit customer consent; automatic sync without consent is prohibited.
