# Use Case: Configure Business

## Overview

**Use Case ID:** UC-007
**Use Case Name:** Configure Business
**Primary Actor:** Salon Owner
**Goal:** Configure business profile, hours, policies, notifications, and calendar connections.
**Status:** Implemented

## Preconditions

- Owner has settings-management permission.
- An active tenant workspace is selected.

## Main Success Scenario

1. Owner opens business settings.
2. System displays current profile, hours, booking rules, notifications, and calendar connections.
3. Owner changes one or more settings.
4. System validates values and identifies any operational impact.
5. Owner confirms the changes.
6. System records the configuration and displays the effective settings, achieving business configuration.

## Alternative Flows

### A1: Working Hours Conflict

**Trigger:** New hours conflict with existing future appointments (step 4)
**Flow:**

1. System identifies affected appointments.
2. Owner revises the hours or explicitly defers the change.
3. Use case continues at step 4.

### A2: External Authorization Is Declined

**Trigger:** Owner does not approve an external calendar connection (step 5)
**Flow:**

1. System leaves that calendar connection disconnected.
2. Use case continues at step 6.

## Postconditions

### Success Postconditions

- Confirmed settings become the tenant's effective business configuration.

### Failure Postconditions

- Invalid or unconfirmed settings are not applied.

## Business Rules

### BR-013: Booking Rules Follow Tenant Time

Working hours and booking policies use the tenant's configured time zone.

### BR-014: Optional Calendar Consent

External calendar connections remain disabled until the owner grants the required consent.
