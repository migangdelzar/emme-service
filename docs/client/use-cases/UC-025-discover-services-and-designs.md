# Use Case: Discover Services and Designs

## Overview

**Use Case ID:** UC-025
**Use Case Name:** Discover Services and Designs
**Primary Actor:** Customer
**Goal:** Browse available salon services and nail designs so that I can choose what to book or request.
**Status:** Implemented

## Preconditions

- Customer is interacting through an approved channel (WhatsApp or web chat).
- The tenant has published services and/or catalog items.

## Main Success Scenario

1. Customer asks to see available services or nail designs.
2. System retrieves the tenant's active service catalog.
3. System presents services with names, durations, categories, descriptions, and prices.
4. Customer asks for nail design inspiration.
5. System retrieves the tenant's nail design catalog with images.
6. Customer submits a reference image of a desired nail design.
7. System analyzes the image and matches it against the tenant's catalog using hybrid semantic and keyword search.
8. System presents the closest matching designs with images, style descriptions, and associated services, achieving design discovery.

## Alternative Flows

### A1: No Matching Designs Found

**Trigger:** The submitted image does not match any catalog item above the confidence threshold (step 7)
**Flow:**

1. System informs the customer that no close matches were found.
2. System suggests the customer describe the desired style in text or browse the full catalog.
3. Use case ends.

### A2: Catalog Is Empty

**Trigger:** The tenant has no published services or catalog items (step 2 or 5)
**Flow:**

1. System informs the customer that the catalog is currently unavailable.
2. System suggests the customer contact the salon directly.
3. Use case ends.

## Postconditions

### Success Postconditions

- Customer has viewed available services and matching designs.
- A matched design can be used as context for a booking request.

### Failure Postconditions

- Customer receives guidance to contact the salon.

## Business Rules

### BR-033: Price and Availability from Structured APIs

Prices displayed during discovery must come from the structured service catalog API, never from AI-generated text or RAG output.

### BR-034: Image Matching Confidence

Catalog matching requires a minimum confidence score; matches below the threshold are not presented.
