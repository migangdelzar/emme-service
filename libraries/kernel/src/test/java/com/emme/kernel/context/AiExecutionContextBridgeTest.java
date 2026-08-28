package com.emme.kernel.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emme.kernel.tracing.CorrelationContextHolder;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class AiExecutionContextBridgeTest {

  @AfterEach
  void clearLegacyContext() {
    TenantContext.clear();
    com.emme.kernel.tracing.CorrelationId.clear();
    MDC.clear();
  }

  @Test
  void bindsBackendContextToLegacyTenantCorrelationAndMdcForAnOperation() {
    AiExecutionContext context = context();

    String result =
        AiExecutionContextScope.call(
            context,
            () ->
                AiExecutionContextBridge.callCurrent(
                    () -> {
                      assertThat(TenantContextHolder.requireCurrentTenantId())
                          .isEqualTo(context.tenantId());
                      assertThat(CorrelationContextHolder.requireCorrelationId())
                          .isEqualTo(context.traceId());
                      assertThat(MDC.get("tenantId")).isEqualTo(context.tenantId().toString());
                      assertThat(MDC.get("correlationId")).isEqualTo(context.traceId());
                      return "complete";
                    }));

    assertThat(result).isEqualTo("complete");
    assertThat(TenantContextHolder.currentTenantOptional()).isEmpty();
    assertThat(com.emme.kernel.tracing.CorrelationId.get()).isNull();
    assertThat(MDC.get("tenantId")).isNull();
    assertThat(MDC.get("correlationId")).isNull();
  }

  @Test
  void restoresLegacyContextAfterTheBridgedOperation() {
    UUID previousTenant = UUID.randomUUID();
    TenantContext.setCurrentTenant(previousTenant);
    com.emme.kernel.tracing.CorrelationId.set("previous-trace");
    MDC.put("tenantId", previousTenant.toString());
    MDC.put("correlationId", "previous-trace");

    AiExecutionContext context = context();
    AiExecutionContextScope.call(context, () -> AiExecutionContextBridge.callCurrent(() -> null));

    assertThat(TenantContextHolder.requireCurrentTenantId()).isEqualTo(previousTenant);
    assertThat(CorrelationContextHolder.requireCorrelationId()).isEqualTo("previous-trace");
    assertThat(MDC.get("tenantId")).isEqualTo(previousTenant.toString());
    assertThat(MDC.get("correlationId")).isEqualTo("previous-trace");
  }

  @Test
  void capturesBothScopedAndLegacyContextForAnOrdinaryExecutor() throws Exception {
    AiExecutionContext context = context();

    try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
      String tenantId =
          AiExecutionContextScope.call(
              context,
              () ->
                  executor
                      .submit(
                          AiExecutionContextBridge.captureCurrent(
                              () -> {
                                assertThat(AiExecutionContextScope.requireCurrent())
                                    .isEqualTo(context);
                                assertThat(TenantContextHolder.requireCurrentTenantId())
                                    .isEqualTo(context.tenantId());
                                assertThat(CorrelationContextHolder.requireCorrelationId())
                                    .isEqualTo(context.traceId());
                                return MDC.get("tenantId");
                              }))
                      .get());

      assertThat(tenantId).isEqualTo(context.tenantId().toString());
    }
  }

  @Test
  void failsClosedWhenCapturingWithoutAnAiContext() {
    assertThatThrownBy(() -> AiExecutionContextBridge.captureCurrent(() -> "value"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No AI execution context");
  }

  private static AiExecutionContext context() {
    return new AiExecutionContext(
        UUID.randomUUID(),
        UUID.randomUUID(),
        Set.of("CLIENT"),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "trace-bridge",
        "idempotency-bridge");
  }
}
