# Use Case: Process Payments

## Overview

**Use Case ID:** UC-016
**Use Case Name:** Process Payments
**Primary Actor:** Salon Owner
**Goal:** Initiate, track, and refund tenant payments through the configured provider.
**Status:** Implemented

## Preconditions

- Owner has billing permission.
- An active tenant workspace is selected.
- A payment provider is configured for the tenant.

## Main Success Scenario

1. Owner requests to initiate a payment.
2. System presents the payment form with required fields.
3. Owner provides the payment amount, currency, and a provider reference.
4. System validates the amount is positive and currency is supported.
5. System submits the payment to the configured provider.
6. Provider returns a transaction identifier and pending status.
7. System records the payment as pending and displays the payment confirmation.
8. Provider later processes the payment and sends a status update.
9. System receives the provider callback, verifies the signature, and updates the payment status.
10. Owner may later review payment details or request a refund on captured payments.
11. System processes the refund through the provider and records the updated status, achieving payment management.

## Alternative Flows

### A1: Duplicate Payment Reference

**Trigger:** Owner submits a provider reference that already exists for this tenant (step 4)
**Flow:**

1. System identifies the duplicate and returns the existing payment details.
2. Use case ends.

### A2: Payment Declined

**Trigger:** Provider declines the payment (step 6)
**Flow:**

1. System records the payment as declined with the provider's reason.
2. Owner may retry with a new provider reference.
3. Use case continues at step 3.

### A3: Refund Not Eligible

**Trigger:** Owner requests a refund on a payment not in CAPTURED state (step 10)
**Flow:**

1. System rejects the refund request and explains the current payment status.
2. Use case ends.

### A4: Webhook Signature Invalid

**Trigger:** Provider callback fails signature verification (step 9)
**Flow:**

1. System rejects the callback with an unauthorized response.
2. The provider may retry the callback.
3. Use case continues at step 9.

## Postconditions

### Success Postconditions

- Payment is recorded with the correct status reflecting provider outcome.
- Provider callback is processed exactly once per event.

### Failure Postconditions

- No payment is created for duplicate references.
- Refund is not processed against non-captured payments.

## Business Rules

### BR-033: Idempotent Payment Initiation

Repeated initiation with the same provider reference must return the existing payment rather than creating a duplicate.

### BR-034: Refund Only Captured Payments

Refunds may only be requested against payments in CAPTURED state.

### BR-035: Provider Callback Verification

All provider callbacks must pass cryptographic signature verification before processing.

### BR-036: Tenant Payment Isolation

Payments are visible and operable only within their owning tenant.

### BR-037: Payment Lifecycle

A payment transitions through an approved lifecycle: PENDING → AUTHORIZED → CAPTURED, with DECLINED and REFUNDED as terminal states from PENDING and CAPTURED respectively.
