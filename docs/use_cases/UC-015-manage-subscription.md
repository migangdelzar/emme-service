# Use Case: Manage Subscription

## Overview

**Use Case ID:** UC-015  
**Use Case Name:** Manage Subscription  
**Primary Actor:** Salon Owner  
**Goal:** Review the tenant subscription and operate within its subscription entitlements.  
**Status:** Draft

## Preconditions

- Owner has billing permission.
- Tenant has a current subscription assignment.

## Main Success Scenario

1. Owner opens subscription management.
2. System displays current subscription, billing status, entitlements, limits, and usage.
3. Owner reviews available subscription actions.
4. Owner selects an eligible change or retains the current subscription.
5. System validates the requested subscription action.
6. Owner confirms the action.
7. System records the subscription outcome and displays effective entitlements, achieving subscription management.

## Alternative Flows

### A1: Subscription Action Is Not Eligible

**Trigger:** Requested subscription action violates eligibility rules (step 5)  
**Flow:**

1. System explains the unmet requirement.
2. Owner selects another action or cancels.
3. Use case continues at step 4.

### A2: Billing State Blocks Change

**Trigger:** Current billing state prevents the requested change (step 5)  
**Flow:**

1. System preserves current entitlements and identifies the required resolution.
2. Use case ends.

## Postconditions

### Success Postconditions

- Subscription and entitlements reflect the confirmed eligible outcome.

### Failure Postconditions

- Existing subscription and entitlements remain unchanged.

## Business Rules

### BR-029: Entitlement Enforcement

Use cases must enforce the tenant's effective subscription and limits at their business boundaries.

### BR-030: Billing State Authority

Subscription changes require a billing state that permits the requested transition.
