# Use Case: Manage Customers

## Overview

**Use Case ID:** UC-005  
**Use Case Name:** Manage Customers  
**Primary Actor:** Staff Member  
**Goal:** Maintain an accurate tenant customer profile and review its service history.  
**Status:** Draft

## Preconditions

- Staff member has customer-management permission.
- An active tenant workspace is selected.

## Main Success Scenario

1. Staff member searches for or starts a customer profile.
2. System displays matching tenant customers or a new profile form.
3. Staff member selects a customer and provides permitted details.
4. System validates contact, preference, and safety information.
5. Staff member confirms the change.
6. System records the profile and displays visit and spending history.
7. Staff member confirms the customer information is ready for use, achieving customer management.

## Alternative Flows

### A1: Possible Duplicate Customer

**Trigger:** Details match an existing customer (step 4)  
**Flow:**

1. System displays the possible existing profile.
2. Staff member selects the existing profile or confirms a distinct customer.
3. Use case continues at step 5.

### A2: Customer Cannot Be Removed

**Trigger:** Customer has retained business history (step 5)  
**Flow:**

1. System offers retirement according to retention policy.
2. Staff member confirms retirement or cancels.
3. Use case ends.

## Postconditions

### Success Postconditions

- Tenant customer information reflects the confirmed change.

### Failure Postconditions

- Existing customer information remains unchanged.

## Business Rules

### BR-009: Customer Tenant Isolation

Customer profiles and history are visible only within their owning tenant.

### BR-010: Safety Information

Authorized staff may record allergies and service preferences needed for safe fulfillment.
