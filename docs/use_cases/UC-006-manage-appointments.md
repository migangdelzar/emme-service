# Use Case: Manage Appointments

## Overview

**Use Case ID:** UC-006  
**Use Case Name:** Manage Appointments  
**Primary Actor:** Staff Member  
**Goal:** Create, reschedule, progress, or cancel a valid appointment without schedule conflicts.  
**Status:** Draft

## Preconditions

- Actor is authorized for the requested appointment operation.
- Customer and active service exist for new bookings.

## Main Success Scenario

1. Actor selects an appointment operation.
2. System displays the appointment and valid actions or available booking details.
3. Actor provides the service, artist, date, and time as applicable.
4. System verifies hours, duration, capability, policy, and availability.
5. Actor confirms the requested change.
6. System records exactly one appointment outcome and updates availability.
7. System displays the resulting appointment state, achieving schedule management.

## Alternative Flows

### A1: Slot Is Unavailable

**Trigger:** Requested slot overlaps another eligible appointment (step 4)  
**Flow:**

1. System rejects the conflicting time and displays current alternatives.
2. Actor selects another slot.
3. Use case continues at step 4.

### A2: State Transition Is Invalid

**Trigger:** Requested status change is not valid from the current state (step 4)  
**Flow:**

1. System preserves the appointment and explains the allowed actions.
2. Use case ends.

## Postconditions

### Success Postconditions

- Appointment and availability reflect exactly one confirmed outcome.

### Failure Postconditions

- Appointment and availability remain consistent and unchanged.

## Business Rules

### BR-011: No Double Booking

One artist or constrained resource cannot hold overlapping active appointments.

### BR-012: Valid Appointment Transition

Appointment status changes must follow the approved lifecycle.
