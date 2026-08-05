# Use Case: Sign In with Social Identity

## Overview

**Use Case ID:** UC-027
**Use Case Name:** Sign In with Social Identity
**Primary Actor:** Customer
**Goal:** Sign in using any Keycloak-brokered identity provider (Google, Apple, Facebook, GitHub, Microsoft, etc.) so that the customer can access their booking history and profile without a separate password.
**Status:** Draft

## Preconditions

- The tenant has customer-facing features enabled.
- Keycloak 26 is configured as the identity broker with at least one social identity provider enabled.
- The identity provider is registered and configured in the Keycloak realm.
- The tenant has a verified Meta WhatsApp Business account or web domain for channel routing.

## Supported Identity Providers (Keycloak 26 Built-In)

| Provider | Protocol | v1 Status |
|---|---|---|
| Google | OpenID Connect | Available |
| Apple | OpenID Connect | Available |
| Facebook | OpenID Connect | Available |
| GitHub | OpenID Connect | Available |
| Microsoft / Azure AD | OpenID Connect | Available |
| Twitter / X | OpenID Connect | Available |
| LinkedIn | OpenID Connect | Available |
| GitLab | OpenID Connect | Available |
| Bitbucket | OpenID Connect | Available |
| Instagram | OpenID Connect | Available |
| PayPal | OpenID Connect | Available |
| Stack Overflow | OpenID Connect | Available |
| Generic OpenID Connect v1.0 | OpenID Connect | Available |
| Generic SAML v2.0 | SAML | Available |

**Note:** The EMME backend delegates all social identity brokering to Keycloak. Adding a new provider requires only Keycloak realm configuration — no backend code changes.

## Main Success Scenario

1. Customer opens the salon's web booking page or WhatsApp chat.
2. System presents sign-in options showing the provider buttons configured for this tenant (e.g., "Continue with Google", "Continue with Apple", "Continue with Facebook").
3. Customer selects a provider and is redirected to Keycloak's identity broker.
4. Keycloak redirects to the selected provider's OAuth consent screen.
5. Customer grants the requested permissions.
6. Keycloak receives the OAuth callback, validates the token, and issues an EMME JWT with the provider identity claim.
7. System links the provider identity to the customer's channel participant (WhatsApp number or web session).
8. System resolves or creates the customer profile using the provider identity reference.
9. System establishes an authenticated session with the customer's profile, booking history, and preferences.
10. System confirms sign-in and presents the customer dashboard, achieving social-identity sign-in.

## Alternative Flows

### A1: Customer Declines Provider Permissions

**Trigger:** Customer cancels the provider OAuth consent screen (step 5)
**Flow:**

1. Keycloak returns to the sign-in options with a safe cancellation message.
2. Customer may choose another provider, phone-based sign-in, or retry.
3. Use case ends.

### A2: Identity Already Linked to Customer Profile

**Trigger:** The provider identity is already linked to a customer profile (step 8)
**Flow:**

1. System resolves the existing customer profile.
2. Use case continues at step 9.

### A3: Provider Already Linked to Different Customer

**Trigger:** The provider identity is already linked to a different customer profile (step 8)
**Flow:**

1. System prompts the customer to confirm profile merging or switching.
2. Customer confirms or cancels.
3. If confirmed, system merges profiles; if cancelled, use case ends.

### A4: Provider Token Validation Fails

**Trigger:** Keycloak cannot validate the provider token (step 6)
**Flow:**

1. Keycloak rejects the sign-in.
2. System displays a safe error message without exposing Keycloak internals.
3. Customer may retry or use an alternative sign-in method.
4. Use case ends.

### A5: Channel Participant Already Linked to Different Customer

**Trigger:** The WhatsApp number or web session is already linked to a different customer profile (step 7)
**Flow:**

1. System prompts the customer to confirm profile merging or switch profiles.
2. Customer confirms or cancels.
3. If confirmed, system merges or switches; if cancelled, use case ends.

## Postconditions

### Success Postconditions

- Customer is authenticated via the selected social identity provider.
- Customer's channel identity (WhatsApp/web) is linked to their customer profile.
- Booking history, preferences, and loyalty data are accessible.

### Failure Postconditions

- No authenticated session is established. Customer may retry any supported provider or use phone-based login.

## Business Rules

### BR-037: Keycloak Identity Brokering

All social identity authentication is delegated to Keycloak 26's identity brokering. The EMME backend consumes only validated Keycloak-issued JWTs and never handles provider tokens directly.

### BR-038: Provider-Agnostic Customer Identity

Customer identity is resolved by provider reference (e.g., `google:12345`, `apple:com.apple.user.67890`). The system uses a provider-agnostic identity model mapped via Keycloak's `sub` claim and provider alias. Adding a new provider requires only Keycloak realm configuration.

### BR-039: Provider Fallback

If a configured social identity provider is unavailable or the customer declines, phone-based authentication remains available as an alternative. Customers may also link multiple providers to the same profile.

### BR-040: Tenant-Scoped Provider Selection

Each tenant may configure which identity providers are displayed to their customers. A tenant may choose to offer a subset, all available providers, or none (phone-only). All 14 Keycloak built-in providers are available from day one — no provider is deferred.
