# Use Case: Manage Tenant Lifecycle

## Overview

**Use Case ID:** UC-002
**Use Case Name:** Manage Tenant Lifecycle
**Primary Actor:** Platform Administrator
**Goal:** Create or change a tenant through a safe and auditable lifecycle transition.
**Status:** Implemented

## Preconditions

- Administrator has platform tenant-management permission.
- Requested tenant details are available.

## Main Success Scenario

1. Administrator selects a tenant lifecycle operation.
2. System displays the required tenant information and valid transitions.
3. Administrator provides or confirms the requested details.
4. System validates uniqueness, subscription rules, and transition eligibility.
5. Administrator confirms the operation.
6. System applies the transition and required defaults.
7. System records the outcome and displays the resulting tenant state, achieving lifecycle management.

## Alternative Flows

### A1: Tenant Details Conflict

**Trigger:** Slug or domain conflicts with an existing tenant (step 4)
**Flow:**

1. System identifies the conflicting field.
2. Administrator corrects the details.
3. Use case continues at step 4.

### A2: Transition Is Not Allowed

**Trigger:** Requested lifecycle transition is invalid (step 4)
**Flow:**

1. System rejects the transition and preserves the current tenant state.
2. Use case ends.

## Postconditions

### Success Postconditions

- Tenant state and configuration reflect the confirmed operation.
- The operation is auditable.

### Failure Postconditions

- Existing tenant state remains unchanged.

## Business Rules

### BR-003: Nails Tenant Scope

Only the Nails application type can be provisioned in v1.

### BR-004: Staged Deletion

Tenant deletion first disables normal traffic and preserves audit information; one action cannot immediately destroy data.
