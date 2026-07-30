# Use Case: Manage Knowledge Sources

## Overview

**Use Case ID:** UC-013  
**Use Case Name:** Manage Knowledge Sources  
**Primary Actor:** Tenant Manager  
**Goal:** Add and maintain approved tenant knowledge for grounded retrieval.  
**Status:** Draft

## Preconditions

- Manager has knowledge-management permission.
- An active tenant workspace is selected.

## Main Success Scenario

1. Manager selects a knowledge-source operation.
2. System displays accepted source types and current source statuses.
3. Manager submits or selects a source.
4. System validates the source and records it for processing.
5. System converts and indexes the source.
6. System marks the source ready and displays its status, achieving knowledge-source management.

## Alternative Flows

### A1: Source Is Invalid

**Trigger:** Source type, size, or content fails validation (step 4)  
**Flow:**

1. System rejects the source and explains accepted limits.
2. Manager submits a corrected source.
3. Use case continues at step 4.

### A2: Processing Fails

**Trigger:** Source cannot be converted or indexed (step 5)  
**Flow:**

1. System marks the source failed with an actionable reason.
2. Manager retries after correcting the cause or retires the source.
3. Use case ends.

## Postconditions

### Success Postconditions

- Approved tenant content is ready for tenant-isolated retrieval.

### Failure Postconditions

- Failed or partially processed content is not presented as ready.

## Business Rules

### BR-025: Ready Content Only

Retrieval may use only sources that completed the approved processing lifecycle.

### BR-026: Tenant Knowledge Isolation

Knowledge sources and retrieved content remain isolated to their owning tenant.
