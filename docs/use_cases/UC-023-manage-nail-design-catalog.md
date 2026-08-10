# Use Case: Manage Nail Design Catalog

## Overview

**Use Case ID:** UC-023
**Use Case Name:** Manage Nail Design Catalog
**Primary Actor:** Salon Manager
**Goal:** Maintain a browsable catalog of nail designs with images so that customers can discover inspiration and match their desired looks.
**Status:** Draft

## Preconditions

- Manager has catalog-management permission.
- An active tenant workspace is selected.

## Main Success Scenario

1. Manager opens the nail design catalog section.
2. System displays existing catalog items as a grid of design cards with images and names.
3. Manager creates a new catalog item by providing a name and uploading one or more images.
4. System stores the images and generates AI-based captions describing the design style, colors, and techniques.
5. System indexes the item for hybrid semantic and keyword search.
6. Manager browses the updated catalog and verifies the new item appears.
7. Manager deletes an outdated catalog item.
8. System removes the item and its images from the catalog, achieving catalog management.

## Alternative Flows

### A1: Image Upload Fails

**Trigger:** Image file is too large or in an unsupported format (step 3)
**Flow:**

1. System rejects the upload and displays supported formats and size limits.
2. Manager selects a valid image.
3. Use case continues at step 3.

### A2: Duplicate Design Detected

**Trigger:** The new catalog item closely matches an existing one (step 4)
**Flow:**

1. System warns that a similar design already exists and shows the match.
2. Manager confirms creation or cancels.
3. If cancelled, use case ends.

## Postconditions

### Success Postconditions

- The catalog reflects the added or removed design items.
- New items are searchable by customers.

### Failure Postconditions

- The catalog remains unchanged.

## Business Rules

### BR-028: Image Size and Format

Catalog images must be JPEG, PNG, or WEBP format and not exceed 10 MB per image.

### BR-029: Tenant-Isolated Catalog

Catalog items are scoped to the tenant; cross-tenant design visibility is prohibited.
