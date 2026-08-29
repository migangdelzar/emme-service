package com.emme.kernel.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.StructuredTaskScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantExecutionContextTest {

  @AfterEach
  void clearLegacyContext() {
    TenantContext.clear();
    com.emme.kernel.tracing.CorrelationId.clear();
    org.slf4j.MDC.clear();
  }

  @Test
  void bindsTenantContextForOneLexicalScopeAndRestoresNestedScopes() {
    var outer = context();
    var inner = context();

    assertThat(TenantExecutionContextScope.current()).isEmpty();
    TenantExecutionContextScope.run(
        outer,
        () -> {
          assertThat(TenantExecutionContextScope.requireCurrent()).isEqualTo(outer);
          TenantExecutionContextScope.run(
              inner,
              () -> assertThat(TenantExecutionContextScope.requireCurrent()).isEqualTo(inner));
          assertThat(TenantExecutionContextScope.requireCurrent()).isEqualTo(outer);
        });
    assertThat(TenantExecutionContextScope.current()).isEmpty();
  }

  @Test
  @SuppressWarnings("preview")
  void structuredTaskScopeChildrenInheritTheImmutableTenantContext() throws Exception {
    var context = context();

    var observed =
        TenantExecutionContextScope.call(
            context,
            () -> {
              try (var scope =
                  StructuredTaskScope.open(
                      StructuredTaskScope.Joiner.<TenantExecutionContext>allSuccessfulOrThrow())) {
                var child = scope.fork(TenantExecutionContextScope::requireCurrent);
                scope.join();
                return child.get();
              }
            });

    assertThat(observed).isEqualTo(context);
  }

  @Test
  void bridgeInstallsLegacyTenantAndCorrelationValuesOnlyForTheOperation() {
    var context = context();

    TenantExecutionContextScope.run(
        context,
        () ->
            TenantContextBridge.runCurrent(
                () -> {
                  assertThat(TenantContext.getCurrentTenantId()).isEqualTo(context.tenantId());
                  assertThat(TenantContext.getCurrentDatabaseId()).isEqualTo(context.databaseId());
                  assertThat(com.emme.kernel.tracing.CorrelationId.get())
                      .isEqualTo(context.correlationId());
                }));

    assertThat(TenantContext.getCurrentTenantId()).isNull();
    assertThat(TenantContext.getCurrentDatabaseId()).isNull();
    assertThat(com.emme.kernel.tracing.CorrelationId.get()).isNull();
  }

  @Test
  void capturesScopedAndLegacyTenantContextForAnOrdinaryExecutor() throws Exception {
    var context = context();

    try (var executor = Executors.newSingleThreadExecutor()) {
      var observedTenant =
          TenantExecutionContextScope.call(
              context,
              () ->
                  executor
                      .submit(
                          TenantContextBridge.captureCurrent(
                              () -> {
                                assertThat(TenantExecutionContextScope.requireCurrent())
                                    .isEqualTo(context);
                                assertThat(TenantContext.getCurrentTenantId())
                                    .isEqualTo(context.tenantId());
                                return TenantContext.getCurrentTenantId();
                              }))
                      .get());

      assertThat(observedTenant).isEqualTo(context.tenantId());
    }
  }

  @Test
  void failsClosedWhenTenantScopeIsMissing() {
    assertThatThrownBy(TenantExecutionContextScope::requireCurrent)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No tenant execution context");
    assertThatThrownBy(TenantContextBridge::requireCurrent)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No tenant execution context");
  }

  private static TenantExecutionContext context() {
    return new TenantExecutionContext(
        UUID.randomUUID(), UUID.randomUUID(), "tenant-trace-" + UUID.randomUUID());
  }
}
