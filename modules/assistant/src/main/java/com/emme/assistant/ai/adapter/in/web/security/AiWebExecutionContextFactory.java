package com.emme.assistant.ai.adapter.in.web.security;

import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.Channel;
import com.emme.kernel.context.TenantContextHolder;
import com.emme.ai.contracts.tenant.AiAuthorizationContextResolver;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;

/** Creates AI context from backend-authenticated identity and tenant state only. */
public final class AiWebExecutionContextFactory {

  private final java.util.Optional<AiAuthorizationContextResolver> authorizationResolver;

  public AiWebExecutionContextFactory() {
    this(java.util.Optional.empty());
  }

  public AiWebExecutionContextFactory(AiAuthorizationContextResolver authorizationResolver) {
    this(java.util.Optional.of(java.util.Objects.requireNonNull(authorizationResolver, "authorizationResolver must not be null")));
  }

  public AiWebExecutionContextFactory(
      java.util.Optional<AiAuthorizationContextResolver> authorizationResolver) {
    this.authorizationResolver = java.util.Objects.requireNonNull(authorizationResolver, "authorizationResolver must not be null");
  }

  public AiExecutionContext forReadOnly(
      String traceId,
      String issuer,
      String subject,
      Collection<? extends GrantedAuthority> authorities) {
    requireText(traceId, "traceId");
    UUID resourceId =
        UUID.nameUUIDFromBytes(("emme-ai-request-v1:" + traceId).getBytes(StandardCharsets.UTF_8));
    UUID tenantId = TenantContextHolder.requireCurrentTenantId();
    var authorization = authorization(tenantId, subject, roles(authorities));
    return new AiExecutionContext(
        tenantId,
        AiPrincipalIdentity.fromTrustedClaims(issuer, subject),
        authorization.roles(),
        resourceId,
        resourceId,
        traceId,
        traceId,
        Channel.WEB,
        authorization.tenantCapabilities(),
        authorization.enabledFeatures());
  }

  public AiExecutionContext forConversation(
      UUID conversationId,
      String traceId,
      String idempotencyKey,
      String issuer,
      String subject,
      Collection<? extends GrantedAuthority> authorities) {
    if (conversationId == null) {
      throw new NullPointerException("conversationId must not be null");
    }
    requireText(traceId, "traceId");
    requireText(idempotencyKey, "idempotencyKey");
    UUID workflowId =
        UUID.nameUUIDFromBytes(
            ("emme-ai-conversation-workflow-v1:" + conversationId + ":" + idempotencyKey)
                .getBytes(StandardCharsets.UTF_8));
    UUID tenantId = TenantContextHolder.requireCurrentTenantId();
    var authorization = authorization(tenantId, subject, roles(authorities));
    return new AiExecutionContext(
        tenantId,
        AiPrincipalIdentity.fromTrustedClaims(issuer, subject),
        authorization.roles(),
        conversationId,
        workflowId,
        traceId,
        idempotencyKey,
        Channel.WEB,
        authorization.tenantCapabilities(),
        authorization.enabledFeatures());
  }

  public AiExecutionContext forReview(
      UUID reviewTaskId,
      String traceId,
      String idempotencyKey,
      String issuer,
      String subject,
      Collection<? extends GrantedAuthority> authorities) {
    if (reviewTaskId == null) {
      throw new NullPointerException("reviewTaskId must not be null");
    }
    UUID tenantId = TenantContextHolder.requireCurrentTenantId();
    var authorization = authorization(tenantId, subject, roles(authorities));
    return new AiExecutionContext(
        tenantId,
        AiPrincipalIdentity.fromTrustedClaims(issuer, subject),
        authorization.roles(),
        reviewTaskId,
        reviewTaskId,
        traceId,
        idempotencyKey,
        Channel.WEB,
        authorization.tenantCapabilities(),
        authorization.enabledFeatures());
  }

  private AiAuthorizationContextResolver.AiAuthorizationContext authorization(
      UUID tenantId, String subject, Set<String> authenticatedRoles) {
    return authorizationResolver
        .map(resolver -> resolver.resolve(tenantId, subject, authenticatedRoles, Channel.WEB))
        .orElseGet(
            () ->
                new AiAuthorizationContextResolver.AiAuthorizationContext(
                    authenticatedRoles, Set.of(), Set.of()));
  }

  private static Set<String> roles(Collection<? extends GrantedAuthority> authorities) {
    return authorities == null
        ? Set.of()
        : authorities.stream()
            .map(GrantedAuthority::getAuthority)
            .filter(role -> role != null && !role.isBlank())
            .collect(Collectors.toUnmodifiableSet());
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
  }
}
