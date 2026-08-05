# Use Case: Deliver Notifications

## Overview

**Use Case ID:** UC-014
**Use Case Name:** Deliver Notifications
**Primary Actor:** Staff Member
**Goal:** Deliver an approved tenant notification and record its outcome.
**Status:** Draft

## Preconditions

- Tenant has an enabled delivery channel.
- A valid notification request and recipient exist.

## Main Success Scenario

1. Staff member or business process requests a notification.
2. System verifies tenant settings, recipient, template, and permission.
3. System prepares the channel-appropriate message.
4. External provider accepts the delivery request.
5. System records the provider outcome.
6. System displays or reports successful delivery handling, achieving notification delivery.

## Alternative Flows

### A1: Channel Is Disabled

**Trigger:** Tenant has disabled the requested channel (step 2)
**Flow:**

1. System records that delivery was not attempted.
2. Use case ends.

### A2: Provider Rejects Delivery

**Trigger:** External provider rejects the message (step 4)
**Flow:**

1. System records the failure and whether retry is permitted.
2. System schedules an eligible retry or requests staff intervention.
3. Use case ends.

## Postconditions

### Success Postconditions

- Notification outcome is recorded exactly once for the request.

### Failure Postconditions

- Failure is visible and no duplicate delivery is treated as a new request.

## Business Rules

### BR-027: Tenant Notification Policy

Delivery must honor tenant channel, template, timing, and consent settings.

### BR-028: Idempotent Delivery Request

Repeated processing of one notification request cannot create multiple logical notifications.
