# Use Case: Complete Conversational Booking

## Overview

**Use Case ID:** UC-011
**Use Case Name:** Complete Conversational Booking
**Primary Actor:** Customer
**Goal:** Confirm and create one valid appointment through conversation.
**Status:** Implemented

## Preconditions

- Customer is in an active tenant conversation.
- A valid service and customer context can be established.

## Main Success Scenario

1. Customer expresses intent to book an appointment.
2. System gathers missing service, date, time, and customer details.
3. System presents valid available choices.
4. Customer selects an option.
5. System presents a complete booking summary and asks for explicit confirmation.
6. Customer confirms the booking.
7. System creates exactly one appointment and communicates confirmation, achieving conversational booking.

## Alternative Flows

### A1: Customer Changes Details

**Trigger:** Customer rejects or modifies the summary (step 6)
**Flow:**

1. System preserves no appointment and gathers the changed detail.
2. Use case continues at step 3.

### A2: Confirmation Expires

**Trigger:** Customer does not confirm before expiry (step 6)
**Flow:**

1. System expires the draft without creating an appointment.
2. Use case ends.

### A3: Slot Becomes Unavailable

**Trigger:** Selected slot is no longer available (step 7)
**Flow:**

1. System does not create a conflicting appointment and presents alternatives.
2. Use case continues at step 3.

## Postconditions

### Success Postconditions

- Exactly one confirmed appointment corresponds to the customer's approved summary.

### Failure Postconditions

- No unconfirmed or conflicting appointment is created.

## Business Rules

### BR-021: Explicit Confirmation

The system must receive explicit customer confirmation before creating the conversational booking.

### BR-022: Booking Draft Expiry

An expired booking draft cannot execute without a new confirmation.
