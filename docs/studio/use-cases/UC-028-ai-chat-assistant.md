# Use Case: Use AI Chat Assistant

## Overview

**Use Case ID:** UC-028
**Use Case Name:** Use AI Chat Assistant
**Primary Actor:** Staff Member
**Goal:** Ask the AI assistant questions and receive grounded answers based on tenant knowledge and salon context.
**Status:** Implemented

## Preconditions

- Actor has ai-chat permission.
- An active tenant workspace is selected.
- The ai_chat feature flag is enabled for the tenant.

## Main Success Scenario

1. Staff member opens the AI chat interface.
2. Staff member types a question or request in natural language.
3. Staff member optionally provides conversation context from a prior exchange.
4. System sends the query to the AI service.
5. System retrieves relevant knowledge from the tenant's indexed documents.
6. System generates a grounded response using the retrieved context.
7. System displays the AI response to the staff member.
8. Staff member may continue the conversation with follow-up questions, achieving AI assistance.

## Alternative Flows

### A1: Knowledge Retrieval Empty

**Trigger:** No relevant tenant documents match the query (step 5)
**Flow:**

1. System generates a response based on general salon knowledge without tenant-specific grounding.
2. Use case continues at step 7.

### A2: Intent Detection

**Trigger:** Staff member requests intent classification of a customer message (step 2)
**Flow:**

1. Staff member submits a message for intent analysis.
2. System detects the intent label, confidence score, and extracted parameters.
3. System displays the classification result.
4. Use case ends.

### A3: Feature Flag Disabled

**Trigger:** The ai_chat feature flag is disabled for the tenant (step 1)
**Flow:**

1. System indicates the AI assistant is unavailable.
2. Use case ends.

## Postconditions

### Success Postconditions

- Staff member has received an AI-generated answer grounded in tenant knowledge.
- No business data is changed by the query.

### Failure Postconditions

- Query fails gracefully with an explanation; no partial or misleading answer is displayed.

## Business Rules

### BR-038: Tenant Knowledge Isolation

AI responses must draw only from the requesting tenant's indexed knowledge sources and general salon domain knowledge. Cross-tenant document access is prohibited.

### BR-039: Retrieval from Ready Sources Only

Only documents that have completed the approved processing lifecycle may contribute to AI responses.

### BR-040: Feature Flag Gate

AI chat availability is controlled by the ai_chat feature flag; the endpoint must reject requests when the flag is disabled.
