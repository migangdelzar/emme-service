# Use Case: Integrate Google Workspace

## Overview

**Use Case ID:** UC-022
**Use Case Name:** Integrate Google Workspace
**Primary Actor:** Salon Owner
**Goal:** Connect Google Calendar and Google Sheets so that appointments sync automatically and business data can be exported.
**Status:** Draft

## Preconditions

- Owner has configuration-management permissions.
- The tenant has Google Workspace integration enabled.
- Owner has a Google account.

## Main Success Scenario

1. Owner opens the Google Workspace settings section.
2. System displays the current connection status and available integrations.
3. Owner initiates Google OAuth authorization.
4. System redirects to Google's consent screen.
5. Owner grants calendar and sheets permissions.
6. System receives the OAuth callback, stores the refresh token, and displays the connected account email.
7. Owner enables automatic calendar sync.
8. System records the sync preference and begins syncing appointments to Google Calendar.
9. Owner selects an export type (appointments, clients, or full) and triggers a Google Sheets export.
10. System creates a spreadsheet in the owner's Google Drive and populates it with the selected data.
11. System displays the spreadsheet link and records it in the spreadsheet list, achieving Google Workspace integration.

## Alternative Flows

### A1: OAuth Authorization Denied

**Trigger:** Owner denies Google permissions (step 5)
**Flow:**

1. System returns to the settings view with the connection status unchanged.
2. Owner may retry authorization.
3. Use case ends.

### A2: Google Account Already Connected

**Trigger:** Owner tries to connect a second Google account (step 3)
**Flow:**

1. System prompts the owner to disconnect the existing account first.
2. Owner confirms disconnection.
3. System revokes the stored token and clears sync state.
4. Use case continues at step 3.

### A3: Sync Conflict Detected

**Trigger:** A Google Calendar event conflicts with an appointment slot during sync (step 8)
**Flow:**

1. System marks the conflicting appointment with CONFLICT status.
2. System surfaces the conflict in the calendar sync state view.
3. Owner resolves the conflict manually.
4. Use case continues.

### A4: Spreadsheet Export Fails

**Trigger:** Google Sheets API returns an error (step 10)
**Flow:**

1. System records the failure and displays a diagnostic message.
2. Owner may retry the export.
3. Use case ends.

## Postconditions

### Success Postconditions

- Google account is connected and authorized.
- Calendar sync is active or the preference is recorded.
- Exported spreadsheets are accessible and listed.

### Failure Postconditions

- No Google account is connected; sync and export remain unavailable.

## Business Rules

### BR-025: OAuth Token Storage

Google OAuth refresh tokens must be stored encrypted and never exposed in client-side configuration or logs.

### BR-026: One Google Account per Tenant

A tenant may connect only one Google account at a time; connecting a new account revokes the previous one.

### BR-027: Spreadsheet Retention

Exported spreadsheets remain accessible as long as the Google account is connected; disconnecting the account does not delete previously created spreadsheets.
