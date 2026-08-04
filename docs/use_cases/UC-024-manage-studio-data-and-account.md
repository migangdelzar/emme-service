# Use Case: Manage Studio Data and Account

## Overview

**Use Case ID:** UC-024
**Use Case Name:** Manage Studio Data and Account
**Primary Actor:** Tenant Owner
**Goal:** Export business data for backup and manage the studio account lifecycle including deletion.
**Status:** Draft

## Preconditions

- Owner has account-management permissions.
- An active tenant workspace is selected.

## Main Success Scenario

1. Owner opens the data management section in settings.
2. System displays available actions: clear cache, export backup, delete account.
3. Owner requests an export backup.
4. System compiles all tenant data (profile, services, clients, appointments, settings) into a JSON file.
5. System triggers a browser download of the backup file.
6. Owner later decides to delete the studio account.
7. System presents a multi-step confirmation requiring the owner to type a confirmation phrase.
8. Owner completes the confirmation.
9. System stages the account for deletion with an audit hold, achieving account management.

## Alternative Flows

### A1: Backup Export Fails

**Trigger:** Data compilation fails due to size or system error (step 4)
**Flow:**

1. System displays an error with a correlation ID for support.
2. Owner may retry.
3. Use case ends.

### A2: Deletion Confirmation Not Matched

**Trigger:** Owner types an incorrect confirmation phrase (step 7)
**Flow:**

1. System rejects the deletion request.
2. Owner may retry or cancel.
3. If cancelled, use case ends.

### A3: Cache Cleared

**Trigger:** Owner chooses to clear the local browser cache (step 3)
**Flow:**

1. System purges local storage and TanStack Query cache.
2. System reloads the application.
3. Use case ends.

## Postconditions

### Success Postconditions

- Backup file is downloaded to the owner's device.
- Account deletion is staged with an audit hold pending final platform administrator review.

### Failure Postconditions

- Data remains as-is; no backup is downloaded or deletion occurs.

## Business Rules

### BR-030: Deletion Audit Hold

Account deletion is staged, not immediate. A platform administrator must approve final destruction.

### BR-031: Backup Excludes System Data

Exported backups contain business data only; platform metadata, feature flags, and system configuration are excluded.

### BR-032: Confirmation Phrase

Account deletion requires typing the tenant name exactly as confirmation; no one-click deletion is allowed.
