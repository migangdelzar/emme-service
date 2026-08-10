# Use Case: Complete Studio Onboarding

## Overview

**Use Case ID:** UC-021
**Use Case Name:** Complete Studio Onboarding
**Primary Actor:** Tenant Owner
**Goal:** Configure the initial business setup so the salon can begin operating on day one.
**Status:** Implemented

## Preconditions

- A tenant has been provisioned by the platform administrator.
- The tenant owner has an active membership with owner permissions.
- The tenant owner is accessing the studio for the first time.

## Main Success Scenario

1. Tenant owner signs in and selects the newly provisioned tenant.
2. System detects first-time access and presents the onboarding walkthrough.
3. Owner provides the business name, owner name, address, and contact details.
4. System saves the business profile.
5. Owner sets weekly operating hours for each day of the week.
6. Owner configures the booking policy: minimum notice, maximum advance, cancellation window, and slot interval.
7. Owner creates initial nail services with names, durations, categories, and prices.
8. Owner optionally connects a Google Calendar account.
9. System confirms that the minimum viable configuration is complete.
10. Owner marks onboarding as complete, achieving studio readiness.

## Alternative Flows

### A1: Owner Skips Onboarding

**Trigger:** Owner chooses to skip the walkthrough (step 2)
**Flow:**

1. System marks onboarding as skipped and presents the dashboard.
2. Owner can return to complete setup through the settings section.
3. Use case ends.

### A2: Required Fields Are Missing

**Trigger:** Owner attempts to complete onboarding without minimum required fields (step 9)
**Flow:**

1. System highlights the incomplete sections.
2. Owner fills the required fields.
3. Use case continues at step 9.

## Postconditions

### Success Postconditions

- Business profile, operating hours, booking policy, and at least one service are configured.
- The salon can accept appointments.

### Failure Postconditions

- Onboarding remains incomplete or skipped; the salon cannot operate until set up.

## Business Rules

### BR-041: Minimum Viable Configuration

A studio must have at minimum: business name, at least one active operating day, a booking policy, and at least one active service before appointments can be created.

### BR-042: Onboarding Persistence

Onboarding progress is saved incrementally; the owner can close and resume without losing entered data.
