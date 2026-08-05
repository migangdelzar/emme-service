# Use Case: Process Payments

## Overview

**Use Case ID:** UC-016
**Use Case Name:** Process Payments
**Primary Actor:** Customer
**Goal:** Complete or reverse one eligible payment with a reconciled outcome.
**Status:** Draft

## Preconditions

- Tenant has an approved payment provider configuration.
- Actor is authorized for the requested payment action.

## Main Success Scenario

1. Actor initiates an eligible payment action.
2. System displays amount, purpose, and applicable terms.
3. Actor confirms the action with the external provider.
4. External provider returns an authenticated outcome.
5. System reconciles the outcome to the correct tenant and business reference.
6. System records exactly one payment state change.
7. System displays the reconciled result, achieving payment processing.

## Alternative Flows

### A1: Payment Is Declined

**Trigger:** External provider declines the action (step 4)
**Flow:**

1. System records the declined outcome without marking payment complete.
2. Actor may choose another permitted payment action.
3. Use case ends.

### A2: Outcome Is Repeated

**Trigger:** Provider repeats an already processed outcome (step 5)
**Flow:**

1. System returns the previously reconciled result without another business effect.
2. Use case ends.

## Postconditions

### Success Postconditions

- Exactly one reconciled payment outcome is associated with the business reference.

### Failure Postconditions

- No unsupported or duplicate payment state change occurs.

## Business Rules

### BR-031: Provider Outcome Authentication

Only authenticated provider outcomes may alter payment state.

### BR-032: Refund Eligibility

Refunds require an authorized actor, a refundable payment, and an amount within the eligible balance.
