# Use Case: Audit Business Activity

## Overview

**Use Case ID:** UC-017
**Use Case Name:** Audit Business Activity
**Primary Actor:** Platform Administrator
**Goal:** Review traceable security-sensitive and consequential business activity.
**Status:** Draft

## Preconditions

- Administrator has audit permission and any required cross-tenant authorization.
- Auditable activity exists within the permitted retention period.

## Main Success Scenario

1. Administrator opens business activity audit.
2. System displays permitted filters for tenant, actor, action, outcome, and time.
3. Administrator selects audit criteria.
4. System retrieves matching safe audit records.
5. Administrator selects an activity record.
6. System displays its traceable context without prohibited sensitive content.
7. Administrator completes the review, achieving business activity audit.

## Alternative Flows

### A1: Cross-Tenant Review Is Not Authorized

**Trigger:** Criteria exceed the administrator's permitted scope (step 4)
**Flow:**

1. System denies the excessive scope and records the denied attempt.
2. Use case ends.

### A2: Audit Content Has Expired

**Trigger:** Requested activity is outside retention (step 4)
**Flow:**

1. System reports that no retained records satisfy the criteria.
2. Use case ends.

## Postconditions

### Success Postconditions

- Administrator has reviewed permitted audit evidence.

### Failure Postconditions

- Restricted or expired activity remains unavailable.

## Business Rules

### BR-033: Required Audit Context

Auditable records include tenant, actor, request correlation, outcome, and time when applicable.

### BR-034: Audit Redaction

Audit records exclude credentials, payment secrets, and unnecessary customer message content.
