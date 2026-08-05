# Use Case: Receive AI Guidance

## Overview

**Use Case ID:** UC-010
**Use Case Name:** Receive AI Guidance
**Primary Actor:** Customer
**Goal:** Receive a tenant-grounded recommendation, price estimate, or policy answer.
**Status:** Draft

## Preconditions

- A normalized customer request and trusted tenant context exist.
- Required catalog or knowledge information is available.

## Main Success Scenario

1. Customer asks for guidance about services, pricing, or policy.
2. System identifies the customer's intent and required business information.
3. System obtains current structured facts and authorized tenant knowledge.
4. System evaluates whether the available information supports an answer.
5. System composes a response that distinguishes facts from estimates.
6. System presents the grounded guidance, achieving the customer's information goal.

## Alternative Flows

### A1: Information Is Insufficient

**Trigger:** Available tenant information cannot support an answer (step 4)
**Flow:**

1. System explains what information is missing and asks a focused question.
2. Use case ends.

### A2: Guidance Provider Is Unavailable

**Trigger:** System cannot complete guidance generation (step 5)
**Flow:**

1. System provides a safe fallback or offers staff follow-up.
2. Use case ends.

## Postconditions

### Success Postconditions

- Customer receives guidance grounded in authorized tenant information.

### Failure Postconditions

- No unsupported claim or consequential business action is produced.

## Business Rules

### BR-019: Structured Fact Authority

Current prices and availability come from authoritative business records, not generated or retrieved prose.

### BR-020: Tenant-Grounded Guidance

Guidance may use only information authorized for the current tenant and actor.
