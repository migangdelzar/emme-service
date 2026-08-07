# Use Case: Manage Service Catalog

## Overview

**Use Case ID:** UC-004
**Use Case Name:** Manage Service Catalog
**Primary Actor:** Salon Manager
**Goal:** Maintain accurate services, prices, durations, and artist capabilities.
**Status:** Implemented

## Preconditions

- Manager has catalog-management permission.
- An active tenant workspace is selected.

## Main Success Scenario

1. Manager opens the service catalog.
2. System displays current services and artist capabilities.
3. Manager chooses to create or modify a service.
4. Manager provides service details and eligible artists.
5. System validates required values and pricing rules.
6. Manager confirms the change.
7. System records the catalog change and displays the updated offering, achieving catalog management.

## Alternative Flows

### A1: Catalog Details Are Invalid

**Trigger:** Required details or pricing are invalid (step 5)
**Flow:**

1. System identifies the fields requiring correction.
2. Manager corrects the service details.
3. Use case continues at step 5.

### A2: Service Has Historical Use

**Trigger:** Manager requests removal of a service referenced by history (step 3)
**Flow:**

1. System offers retirement instead of destructive removal.
2. Manager confirms retirement.
3. Use case continues at step 7.

## Postconditions

### Success Postconditions

- Current catalog and artist capabilities reflect the confirmed change.

### Failure Postconditions

- Existing catalog information remains unchanged.

## Business Rules

### BR-007: Explicit Service Model

Every offered service has explicit duration, category, description, and pricing information.

### BR-008: Preserve Catalog History

Services referenced by business history are retired rather than destructively removed.
