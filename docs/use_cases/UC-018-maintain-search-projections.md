# Use Case: Maintain Search Projections

## Overview

**Use Case ID:** UC-018  
**Use Case Name:** Maintain Search Projections  
**Primary Actor:** System Operator  
**Goal:** Keep tenant vector and graph search projections current and rebuildable from authoritative records.  
**Status:** Draft

## Preconditions

- Authoritative tenant records exist.
- Operator has projection-operations permission for manual actions.

## Main Success Scenario

1. System detects a committed business change relevant to search.
2. System identifies the tenant, business record, operation, and version.
3. System obtains the current authoritative information.
4. System updates the applicable vector and graph projections once.
5. System records projection progress and freshness.
6. Operator observes healthy projection status, achieving maintained search projections.

## Alternative Flows

### A1: Projection Update Fails

**Trigger:** Projection cannot be updated (step 4)  
**Flow:**

1. System preserves the authoritative business change and marks projection status failed.
2. System retries according to policy and alerts when failure persists.
3. Use case ends.

### A2: Projection Drift Is Detected

**Trigger:** Reconciliation finds missing, stale, or corrupt projection data (step 5)  
**Flow:**

1. Operator selects the affected record, tenant, or projection scope.
2. System rebuilds the selected projection from authoritative records.
3. Use case continues at step 5.

## Postconditions

### Success Postconditions

- Search projections match the latest processed authoritative versions within the freshness target.

### Failure Postconditions

- Authoritative records remain unchanged and stale projection status is visible.

## Business Rules

### BR-035: Projection Is Derived

Vector and graph projections are disposable read models and cannot become authoritative business stores.

### BR-036: Idempotent Projection

Repeating one projection-relevant change cannot produce duplicate logical projection content.
