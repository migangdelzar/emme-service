# Use Case: Review Business Performance

## Overview

**Use Case ID:** UC-003
**Use Case Name:** Review Business Performance
**Primary Actor:** Salon Owner
**Goal:** Review tenant-scoped operational and financial performance for a selected period.
**Status:** Draft

## Preconditions

- Owner has dashboard and financial-report permissions.
- An active tenant workspace is selected.

## Main Success Scenario

1. Owner opens business performance.
2. System displays the current period and available filters.
3. Owner selects a date range and comparison period.
4. System calculates appointments, revenue, completion, and average-ticket measures.
5. System displays summaries and trends.
6. Owner optionally requests an export.
7. System provides the approved report, achieving performance review.

## Alternative Flows

### A1: No Activity Exists

**Trigger:** Selected period has no eligible activity (step 4)
**Flow:**

1. System displays zero-valued measures and an empty-state explanation.
2. Use case continues at step 6.

### A2: Export Cannot Be Produced

**Trigger:** Export generation fails (step 7)
**Flow:**

1. System preserves the displayed report and explains that export is unavailable.
2. Use case ends.

## Postconditions

### Success Postconditions

- Owner has reviewed or exported tenant-scoped performance information.

### Failure Postconditions

- No business data is changed.

## Business Rules

### BR-005: Completed Revenue

Revenue measures include only appointment and payment states approved for financial reporting.

### BR-006: Tenant-Scoped Reporting

Reports and exports contain information from the selected tenant only.
