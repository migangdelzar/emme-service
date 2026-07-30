# Use Case: Normalize Multimodal Input

## Overview

**Use Case ID:** UC-009  
**Use Case Name:** Normalize Multimodal Input  
**Primary Actor:** Customer  
**Goal:** Convert supported text, voice, and image content into one understandable request.  
**Status:** Draft

## Preconditions

- Customer has initiated a valid conversation.
- Submitted media is accessible and within accepted limits.

## Main Success Scenario

1. Customer submits text, voice, images, or a supported combination.
2. System validates the media type, quantity, and size.
3. System converts voice into text and images into structured descriptions as applicable.
4. System combines typed instructions with converted content.
5. System applies customer refinements to the interpreted features.
6. System records confidence and produces one normalized request, achieving multimodal normalization.

## Alternative Flows

### A1: Media Is Unsupported

**Trigger:** Media fails type, quantity, or size validation (step 2)  
**Flow:**

1. System rejects only the unsupported media and explains accepted limits.
2. Customer submits supported content.
3. Use case continues at step 2.

### A2: Interpretation Confidence Is Low

**Trigger:** Converted content is below the approved confidence threshold (step 6)  
**Flow:**

1. System marks uncertain details and asks the customer to clarify.
2. Use case ends.

## Postconditions

### Success Postconditions

- One normalized tenant-scoped request is ready for intent handling.

### Failure Postconditions

- Unsupported or uncertain content causes no consequential action.

## Business Rules

### BR-017: Customer Refinement Priority

Explicit customer text refinements override conflicting inferred image features.

### BR-018: Media Limits

Only approved media types, quantities, and sizes may enter normalization.
