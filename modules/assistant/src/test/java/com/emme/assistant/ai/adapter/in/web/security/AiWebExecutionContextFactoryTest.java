package com.emme.assistant.ai.adapter.in.web.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import com.emme.kernel.context.TenantContextHolder;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class AiWebExecutionContextFactoryTest {

  @Test
  void derivesTenantAndPrincipalFromBackendSecurityContext() {
    UUID tenantId = UUID.randomUUID();
    UUID reviewTaskId = UUID.randomUUID();
    AiWebExecutionContextFactory factory = new AiWebExecutionContextFactory();

    AiExecutionContext context =
        TenantContextHolder.withTenantOverride(
            tenantId,
            () ->
                factory.forReview(
                    reviewTaskId,
                    "trace-1",
                    "review-1",
                    "https://issuer",
                    "auth0|staff-1",
                    List.of(new SimpleGrantedAuthority("ROLE_tenant_staff"))));

    assertThat(context.tenantId()).isEqualTo(tenantId);
    assertThat(context.principalId())
        .isEqualTo(AiPrincipalIdentity.fromTrustedClaims("https://issuer", "auth0|staff-1"));
    assertThat(context.roles()).containsExactly("ROLE_tenant_staff");
    assertThat(context.conversationId()).isEqualTo(reviewTaskId);
    assertThat(context.workflowId()).isEqualTo(reviewTaskId);
    assertThat(AiExecutionContextScope.current()).isEmpty();
  }

  @Test
  void doesNotAcceptAFrontendTenantOverride() {
    AiWebExecutionContextFactory factory = new AiWebExecutionContextFactory();
    UUID trustedTenant = UUID.randomUUID();

    AiExecutionContext context =
        TenantContextHolder.withTenantOverride(
            trustedTenant,
            () ->
                factory.forReview(
                    UUID.randomUUID(),
                    "trace-1",
                    "review-1",
                    "https://issuer",
                    "staff-1",
                    Set.of()));

    assertThat(context.tenantId()).isEqualTo(trustedTenant);
  }

  @Test
  void createsAStableEphemeralReadOnlyCorrelationFromTheBackendTrace() {
    AiWebExecutionContextFactory factory = new AiWebExecutionContextFactory();
    UUID tenantId = UUID.randomUUID();

    AiExecutionContext first =
        TenantContextHolder.withTenantOverride(
            tenantId,
            () ->
                factory.forReadOnly(
                    "trace-read-1",
                    "https://issuer",
                    "auth0|client-1",
                    List.of(new SimpleGrantedAuthority("ROLE_tenant_client"))));
    AiExecutionContext second =
        TenantContextHolder.withTenantOverride(
            tenantId,
            () ->
                factory.forReadOnly(
                    "trace-read-1",
                    "https://issuer",
                    "auth0|client-1",
                    List.of(new SimpleGrantedAuthority("ROLE_tenant_client"))));

    assertThat(first.conversationId()).isEqualTo(second.conversationId());
    assertThat(first.workflowId()).isEqualTo(first.conversationId());
    assertThat(first.idempotencyKey()).isEqualTo("trace-read-1");
  }

  @Test
  void createsConversationContextFromTheTrustedTenantAndIdempotencyKey() {
    AiWebExecutionContextFactory factory = new AiWebExecutionContextFactory();
    UUID tenantId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();

    AiExecutionContext first =
        TenantContextHolder.withTenantOverride(
            tenantId,
            () ->
                factory.forConversation(
                    conversationId,
                    "trace-conversation-1",
                    "conversation-turn-1",
                    "https://issuer",
                    "auth0|client-1",
                    Set.of()));
    AiExecutionContext second =
        TenantContextHolder.withTenantOverride(
            tenantId,
            () ->
                factory.forConversation(
                    conversationId,
                    "trace-conversation-1",
                    "conversation-turn-1",
                    "https://issuer",
                    "auth0|client-1",
                    Set.of()));

    assertThat(first.tenantId()).isEqualTo(tenantId);
    assertThat(first.conversationId()).isEqualTo(conversationId);
    assertThat(first.workflowId()).isEqualTo(second.workflowId());
    assertThat(first.idempotencyKey()).isEqualTo("conversation-turn-1");
  }
}
