# Use Case: Converse Through Channels

## Overview

**Use Case ID:** UC-008
**Use Case Name:** Converse Through Channels
**Primary Actor:** Customer
**Goal:** Exchange messages with the salon through an approved channel using shared conversation behavior.
**Status:** Implemented

## Preconditions

- The channel is bound to an active tenant.
- The incoming interaction can be authenticated when the channel requires it.

## Main Success Scenario

1. Customer sends a message through an approved channel.
2. System identifies the tenant and verifies the incoming interaction.
3. System accepts the message once and establishes conversation context.
4. System determines the requested conversational capability.
5. System prepares a safe tenant-scoped response.
6. System returns the response through the originating channel, achieving the conversation exchange.

## Alternative Flows

### A1: Interaction Cannot Be Verified

**Trigger:** Incoming interaction fails authenticity checks (step 2)
**Flow:**

1. System rejects the interaction without processing customer content.
2. Use case ends.

### A2: Message Is a Retry

**Trigger:** Message was already accepted (step 3)
**Flow:**

1. System returns the prior safe outcome when available.
2. Use case ends.

## Postconditions

### Success Postconditions

- One tenant-scoped conversation message and response outcome are recorded.

### Failure Postconditions

- No duplicate or unauthenticated business action occurs.

## Business Rules

### BR-015: Direct WhatsApp Channel

Production WhatsApp interactions use the salon's direct Meta WhatsApp Cloud API integration.

### BR-016: Shared Conversation Behavior

WhatsApp and web chat use the same business conversation capabilities after channel normalization.
