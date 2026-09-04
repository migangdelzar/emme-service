package com.emme.identity.adapter.out.ai;

import com.emme.ai.contracts.tenant.AiAuthorizationContextResolver;
import com.emme.ai.contracts.tenant.AiAuthorizationContextResolver.AiAuthorizationContext;
import com.emme.identity.api.query.GetCurrentUserMembershipsQuery;
import com.emme.identity.api.result.MembershipDetails;
import com.emme.identity.api.usecase.GetCurrentUserMembershipsUseCase;
import com.emme.identity.application.authorization.FeatureFlagEvaluator;
import com.emme.identity.application.port.out.CustomerMembershipRepository;
import com.emme.identity.application.port.out.SubscriptionPlanPort;
import com.emme.kernel.context.Channel;
import com.emme.subscriptions.api.SubscriptionEntitlementPolicy;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Builds the real tenant authorization envelope used by web and WhatsApp AI requests. */
@Component
public final class IdentityAiAuthorizationContextResolver
    implements AiAuthorizationContextResolver {

  private final GetCurrentUserMembershipsUseCase memberships;
  private final SubscriptionPlanPort subscriptionPlans;
  private final FeatureFlagEvaluator features;
  private final CustomerMembershipRepository customerMemberships;

  public IdentityAiAuthorizationContextResolver(
      GetCurrentUserMembershipsUseCase memberships,
      SubscriptionPlanPort subscriptionPlans,
      FeatureFlagEvaluator features,
      CustomerMembershipRepository customerMemberships) {
    this.memberships = Objects.requireNonNull(memberships, "memberships must not be null");
    this.subscriptionPlans =
        Objects.requireNonNull(subscriptionPlans, "subscriptionPlans must not be null");
    this.features = Objects.requireNonNull(features, "features must not be null");
    this.customerMemberships =
        Objects.requireNonNull(customerMemberships, "customerMemberships must not be null");
  }

  @Override
  public AiAuthorizationContext resolve(
      UUID tenantId, String principalReference, Set<String> authenticatedRoles, Channel channel) {
    Objects.requireNonNull(tenantId, "tenantId must not be null");
    Objects.requireNonNull(channel, "channel must not be null");
    if (principalReference == null || principalReference.isBlank()) {
      throw new IllegalArgumentException("principalReference must not be blank");
    }
    Set<String> authenticated =
        authenticatedRoles == null ? Set.of() : Set.copyOf(authenticatedRoles);
    Set<String> validRoles =
        switch (channel) {
          case WEB -> webRoles(tenantId, principalReference, authenticated);
          case WHATSAPP -> whatsappRoles(tenantId, principalReference, authenticated);
          default -> Set.of();
        };
    Set<String> capabilities = new HashSet<>();
    subscriptionPlans
        .findPlanForTenant(tenantId)
        .map(SubscriptionEntitlementPolicy::getEntitlements)
        .ifPresent(
            entitlements -> {
              capabilities.addAll(entitlements);
              if (entitlements.contains("services:read")) capabilities.add("service_catalog");
              if (entitlements.contains("appointments:read")) capabilities.add("appointments");
            });
    Set<String> enabledFeatures =
        features.getEffective(tenantId).entrySet().stream()
            .filter(entry -> Boolean.TRUE.equals(entry.getValue()))
            .map(entry -> entry.getKey().toLowerCase(Locale.ROOT))
            .collect(Collectors.toUnmodifiableSet());
    return new AiAuthorizationContext(validRoles, capabilities, enabledFeatures);
  }

  private Set<String> webRoles(
      UUID tenantId, String principalReference, Set<String> authenticated) {
    Set<String> membershipsForTenant =
        memberships.getMemberships(new GetCurrentUserMembershipsQuery(principalReference)).stream()
            .filter(details -> tenantId.equals(details.tenantId()))
            .map(MembershipDetails::roleCode)
            .filter(Objects::nonNull)
            .map(IdentityAiAuthorizationContextResolver::canonicalRole)
            .collect(Collectors.toUnmodifiableSet());
    return authenticated.stream()
        .filter(
            role ->
                canonicalRole(role).equals("admin")
                    || membershipsForTenant.contains(canonicalRole(role)))
        .collect(Collectors.toUnmodifiableSet());
  }

  private Set<String> whatsappRoles(
      UUID tenantId, String principalReference, Set<String> authenticated) {
    UUID customerId;
    try {
      customerId = UUID.fromString(principalReference);
    } catch (IllegalArgumentException ignored) {
      return Set.of();
    }
    if (!customerMemberships.existsByCustomerIdAndTenantId(customerId, tenantId)) return Set.of();
    return authenticated.stream()
        .filter(role -> canonicalRole(role).equals("client"))
        .collect(Collectors.toUnmodifiableSet());
  }

  private static String canonicalRole(String role) {
    if (role == null) return "";
    String canonical = role.startsWith("ROLE_") ? role.substring(5) : role;
    return canonical.toLowerCase(Locale.ROOT);
  }
}
