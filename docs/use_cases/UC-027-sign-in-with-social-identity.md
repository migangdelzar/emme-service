# Use Case: Sign In with Social Identity

## Overview

**Use Case ID:** UC-027
**Use Case Name:** Sign In with Social Identity
**Primary Actor:** Customer
**Goal:** Sign in using a Google account (and later Facebook, Apple, Twitter) so that the customer can access their booking history and profile without managing a separate password.
**Status:** Draft

## Preconditions

- The tenant has customer-facing features enabled.
- The tenant has a verified Meta WhatsApp Business account or web domain for channel routing.
- The identity provider integration is configured for the tenant.

## Main Success Scenario

1. Customer opens the salon's web booking page or WhatsApp chat.
2. System presents sign-in options including "Continue with Google".
3. Customer selects Google and is redirected to Google's OAuth consent screen.
4. Customer grants permission to share name and email.
5. System receives the OAuth callback, verifies the token, and resolves or creates the customer identity.
6. System links the customer identity to the channel participant (WhatsApp number or web session).
7. System establishes an authenticated session with the customer's profile, booking history, and preferences.
8. System confirms sign-in and presents the customer dashboard, achieving social-identity sign-in.

## Alternative Flows

### A1: Customer Denies Google Permissions

**Trigger:** Customer cancels the Google OAuth consent screen (step 4)
**Flow:**

1. System returns to the sign-in options.
2. Customer may choose phone-based sign-in or retry.
3. Use case ends.

### A2: Identity Already Linked

**Trigger:** The Google identity is already linked to a customer profile (step 5)
**Flow:**

1. System resolves the existing customer profile.
2. Use case continues at step 7.

### A3: Google Token Verification Fails

**Trigger:** System cannot verify the Google token (step 5)
**Flow:**

1. System rejects the sign-in and displays a safe error message.
2. Customer may retry or use phone-based sign-in.
3. Use case ends.

### A4: Channel Participant Already Linked to Different Customer

**Trigger:** The WhatsApp number or web session is already linked to a different customer profile (step 6)
**Flow:**

1. System prompts the customer to confirm profile merging or switch profiles.
2. Customer confirms or cancels.
3. If confirmed, system merges or switches; if cancelled, use case ends.

## Postconditions

### Success Postconditions

- Customer is authenticated and their channel identity is linked to their customer profile.
- Booking history, preferences, and loyalty data are accessible.

### Failure Postconditions

- No authenticated session is established. Customer may retry Google login or use phone-based login.

## Business Rules

### BR-037: Provider-Agnostic Identity

Customer identity is resolved by provider reference (e.g., Google subject ID). The system uses a provider-agnostic identity model that can support Google, Facebook, Apple, and Twitter without changing the customer profile schema.

### BR-038: Provider Fallback

If Google OAuth is unavailable or the customer declines, phone-based authentication remains available as an alternative sign-in method.

### BR-039: v1 Provider Scope

Google is the only supported social identity provider in v1. Facebook, Apple, and Twitter are explicitly deferred to future releases.
