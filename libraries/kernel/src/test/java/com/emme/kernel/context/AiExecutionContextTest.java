package com.emme.kernel.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class AiExecutionContextTest {

  @Test
  void defensivelyCopiesRolesAndPreservesTheAuthenticatedContext() {
    Set<String> mutableRoles = new HashSet<>(Set.of("tenant_staff"));
    AiExecutionContext context = context(mutableRoles);

    mutableRoles.add("tenant_owner");

    assertThat(context.tenantId())
        .isEqualTo(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
    assertThat(context.roles()).containsExactly("tenant_staff");
    assertThatThrownBy(() -> context.roles().add("tenant_owner"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void rejectsIncompleteOrInvalidSecurityContext() {
    assertThatThrownBy(
            () ->
                new AiExecutionContext(
                    null,
                    PRINCIPAL_ID,
                    Set.of("tenant_staff"),
                    CONVERSATION_ID,
                    WORKFLOW_ID,
                    "trace-1",
                    "idempotency-1"))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("tenantId must not be null");

    assertThatThrownBy(() -> context(Set.of(" ")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("roles must not contain blank values");
  }

  @Test
  void bindsTheContextForTheLexicalScopeOnly() {
    AiExecutionContext context = context(Set.of("tenant_owner"));

    assertThat(AiExecutionContextScope.current()).isEmpty();
    assertThat(AiExecutionContextScope.call(context, AiExecutionContextScope::requireCurrent))
        .isEqualTo(context);
    assertThat(AiExecutionContextScope.current()).isEmpty();
  }

  @Test
  void capturesTheCurrentContextForAnOrdinaryExecutor() throws Exception {
    AiExecutionContext context = context(Set.of("tenant_staff"));

    try (var executor = Executors.newSingleThreadExecutor()) {
      AiExecutionContext observed =
          AiExecutionContextScope.call(
              context,
              () -> {
                Callable<AiExecutionContext> task =
                    AiExecutionContextScope.captureCurrent(AiExecutionContextScope::requireCurrent);
                return executor.submit(task).get();
              });

      assertThat(observed).isEqualTo(context);
    }
  }

  @Test
  void requiresAContextAtTheExecutionBoundary() {
    assertThatThrownBy(AiExecutionContextScope::requireCurrent)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No AI execution context");
  }

  private static final UUID PRINCIPAL_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
  private static final UUID CONVERSATION_ID =
      UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
  private static final UUID WORKFLOW_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

  private static AiExecutionContext context(Set<String> roles) {
    return new AiExecutionContext(
        UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
        PRINCIPAL_ID,
        roles,
        CONVERSATION_ID,
        WORKFLOW_ID,
        "trace-1",
        "idempotency-1");
  }
}
