# Use Case: Access Tenant Workspace

## Overview

**Use Case ID:** UC-001
**Use Case Name:** Access Tenant Workspace
**Primary Actor:** Platform User
**Goal:** Enter an authorized active tenant workspace with the correct permissions.
**Status:** Draft

## Preconditions

- The user has an active account.
- At least one active tenant membership exists unless the user is a platform administrator.

## Main Success Scenario

1. User asks to sign in.
2. System verifies the credentials and required additional authentication.
3. System identifies the user's active tenant memberships.
4. User selects a tenant when more than one active membership exists.
5. System confirms the selected tenant and permissions.
6. System displays the authorized tenant workspace, achieving workspace access.

## Alternative Flows

### A1: Credentials Cannot Be Verified

**Trigger:** System cannot verify the credentials (step 2)
**Flow:**

1. System denies access and displays a safe explanation.
2. Use case ends.

### A2: Tenant Is Unavailable

**Trigger:** Selected membership is inactive or tenant is suspended (step 5)
**Flow:**

1. System removes the unavailable tenant from the valid choices.
2. User selects another active tenant if available.
3. Use case ends.

## Postconditions

### Success Postconditions

- One active tenant workspace and permission set are established.

### Failure Postconditions

- No tenant workspace is established and protected information remains unavailable.

## Business Rules

### BR-001: Active Membership Required

Normal tenant access requires an active membership in an active tenant.

### BR-002: Administrator Additional Authentication

Platform administrators must complete the configured additional authentication requirement.
