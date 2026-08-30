package com.emme.assistant.ai.adapter.in.web.security;

import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.TenantContextHolder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;

/** Creates AI context from backend-authenticated identity and tenant state only. */
public final class AiWebExecutionContextFactory {

  public AiExecutionContext forReadOnly(
      String traceId,
      String issuer,
      String subject,
      Collection<? extends GrantedAuthority> authorities) {
    requireText(traceId, "traceId");
    UUID resourceId =
        UUID.nameUUIDFromBytes(("emme-ai-request-v1:" + traceId).getBytes(StandardCharsets.UTF_8));
    return new AiExecutionContext(
        TenantContextHolder.requireCurrentTenantId(),
        AiPrincipalIdentity.fromTrustedClaims(issuer, subject),
        roles(authorities),
        resourceId,
        resourceId,
        traceId,
        traceId);
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
    return new AiExecutionContext(
        TenantContextHolder.requireCurrentTenantId(),
        AiPrincipalIdentity.fromTrustedClaims(issuer, subject),
        roles(authorities),
        conversationId,
        workflowId,
        traceId,
        idempotencyKey);
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
    return new AiExecutionContext(
        TenantContextHolder.requireCurrentTenantId(),
        AiPrincipalIdentity.fromTrustedClaims(issuer, subject),
        roles(authorities),
        reviewTaskId,
        reviewTaskId,
        traceId,
        idempotencyKey);
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
