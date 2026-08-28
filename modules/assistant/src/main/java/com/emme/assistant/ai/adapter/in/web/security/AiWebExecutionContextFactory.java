package com.emme.assistant.ai.adapter.in.web.security;

import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.TenantContextHolder;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;

/** Creates AI context from backend-authenticated identity and tenant state only. */
public final class AiWebExecutionContextFactory {

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
    Set<String> roles =
        authorities == null
            ? Set.of()
            : authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(role -> role != null && !role.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    return new AiExecutionContext(
        TenantContextHolder.requireCurrentTenantId(),
        AiPrincipalIdentity.fromTrustedClaims(issuer, subject),
        roles,
        reviewTaskId,
        reviewTaskId,
        traceId,
        idempotencyKey);
  }
}
