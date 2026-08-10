# Use Case: Manage Platform Memberships

## Overview

**Use Case ID:** UC-020
**Use Case Name:** Manage Platform Memberships
**Primary Actor:** Platform Administrator
**Goal:** Assign, view, and revoke tenant memberships and permissions across the platform.
**Status:** Implemented

## Preconditions

- Administrator has platform-admin permissions.
- The target tenant exists and is not in DELETED status.
- The target user identity is registered in the identity provider.

## Main Success Scenario

1. Administrator opens the membership management view for a tenant or user.
2. System displays current memberships with role, status, and assignment date.
3. Administrator assigns a membership by selecting a user identity, tenant, and role.
4. System validates that the user identity exists and the membership is not a duplicate.
5. System records the membership with ACTIVE status.
6. Administrator views the user's effective permissions derived from the assigned role.
7. Administrator revokes an existing membership.
8. System changes the membership status to REVOKED and records the revocation, achieving membership management.

## Alternative Flows

### A1: Duplicate Membership

**Trigger:** Administrator attempts to assign a membership that already exists for the same user, tenant, and role (step 4)
**Flow:**

1. System displays the existing membership details.
2. Use case ends.

### A2: Tenant Is Suspended

**Trigger:** Administrator attempts to assign a membership to a suspended tenant (step 4)
**Flow:**

1. System warns that the tenant is suspended and new assignments may not be effective.
2. Administrator confirms or cancels.
3. If confirmed, system records the membership as ACTIVE.
4. Use case continues at step 5.

## Postconditions

### Success Postconditions

- The membership assignment or revocation is recorded.
- The affected user's effective access reflects the change on their next token refresh.

### Failure Postconditions

- No membership change occurs.

## Business Rules

### BR-021: One Active Membership per Role per Tenant

A user identity may have only one active membership for a given role within a tenant. Assigning a new membership of the same role succeeds only if the previous one is revoked.

### BR-022: Revocation Is Permanent

Revoked memberships cannot be reactivated; a new membership assignment is required.
