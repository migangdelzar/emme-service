package com.emme.assistant.ai.application.semantic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.emme.ai.contracts.semantic.SemanticCacheDependencyChanged;
import com.emme.ai.contracts.tenant.AiTenantContextResolver;
import com.emme.assistant.ai.application.port.out.SemanticCacheHotStore;
import com.emme.assistant.ai.application.port.out.SemanticCachePort;
import com.emme.assistant.ai.application.port.out.SemanticMetrics;
import com.emme.assistant.ai.application.trace.AiSemanticExecutionTrace;
import com.emme.assistant.ai.application.trace.AiTraceRecorder;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import com.emme.kernel.context.TenantContextHolder;
import com.emme.kernel.context.TenantExecutionContext;
import com.emme.kernel.context.TenantExecutionContextScope;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SemanticCacheInvalidationServiceTest {

  @Test
  void invalidatesDurableAndHotEntriesForTheChangedTenantDependency() {
    SemanticCachePort durable = mock(SemanticCachePort.class);
    SemanticCacheHotStore hot = mock(SemanticCacheHotStore.class);
    SemanticCacheInvalidationService service =
        new SemanticCacheInvalidationService(
            durable,
            java.util.Optional.of(hot),
            mock(SemanticMetrics.class),
            mock(AiTraceRecorder.class),
            java.util.Optional.of(tenantContextResolver()));
    UUID tenantId = UUID.randomUUID();
    SemanticCacheDependencyChanged event =
        new SemanticCacheDependencyChanged(
            UUID.randomUUID(),
            tenantId,
            null,
            SemanticCacheDependencyChanged.Dependency.PRICE,
            "price-v2",
            Instant.parse("2026-08-31T12:00:00Z"));

    service.invalidate(event);

    var invalidation =
        new SemanticCacheInvalidation(
            tenantId, null, "CHAT_INFORMATIONAL", event.dependency(), event.version());
    verify(durable).invalidate(invalidation);
    verify(hot).invalidate(invalidation);
  }

  @Test
  void establishesTheBackendTenantContextBeforeDurableTenantWideInvalidation() {
    SemanticCachePort durable = mock(SemanticCachePort.class);
    SemanticCacheHotStore hot = mock(SemanticCacheHotStore.class);
    UUID tenantId = UUID.randomUUID();
    SemanticCacheDependencyChanged event = tenantWideEvent(tenantId);
    org.mockito.Mockito.doAnswer(
            invocation -> {
              assertThat(AiExecutionContextScope.requireCurrent().tenantId()).isEqualTo(tenantId);
              assertThat(TenantContextHolder.requireCurrentTenantId()).isEqualTo(tenantId);
              return null;
            })
        .when(durable)
        .invalidate(any(SemanticCacheInvalidation.class));
    AiTraceRecorder traces = mock(AiTraceRecorder.class);
    SemanticCacheInvalidationService service =
        new SemanticCacheInvalidationService(
            durable,
            java.util.Optional.of(hot),
            mock(SemanticMetrics.class),
            traces,
            java.util.Optional.of(tenantContextResolver()));

    service.invalidate(event);

    var trace = org.mockito.ArgumentCaptor.forClass(AiSemanticExecutionTrace.class);
    verify(traces).recordSemanticOutcome(trace.capture());
    assertThat(trace.getValue().principalId()).isEqualTo(new UUID(0, 0));
  }

  @Test
  void resolvesTheDatabaseWhenTheBoundTenantContextHasNoDatabase() {
    UUID tenantId = UUID.randomUUID();
    UUID databaseId = UUID.randomUUID();
    SemanticCachePort durable = mock(SemanticCachePort.class);
    org.mockito.Mockito.doAnswer(
            invocation -> {
              assertThat(TenantContextHolder.currentDatabaseOptional()).contains(databaseId);
              return null;
            })
        .when(durable)
        .invalidate(any(SemanticCacheInvalidation.class));
    AiTenantContextResolver contextResolver = mock(AiTenantContextResolver.class);
    org.mockito.Mockito.when(
            contextResolver.resolve(
                org.mockito.ArgumentMatchers.eq(tenantId),
                org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(new TenantExecutionContext(tenantId, databaseId, "resolved-correlation"));
    SemanticCacheInvalidationService service =
        new SemanticCacheInvalidationService(
            durable,
            java.util.Optional.empty(),
            mock(SemanticMetrics.class),
            mock(AiTraceRecorder.class),
            java.util.Optional.of(contextResolver));

    TenantExecutionContextScope.run(
        new TenantExecutionContext(tenantId, null, "bound-correlation"),
        () -> service.invalidate(tenantWideEvent(tenantId)));

    verify(durable).invalidate(any(SemanticCacheInvalidation.class));
  }

  @Test
  void routesDurableInvalidationThroughTheTenantDatabaseFromTheRegistry() {
    SemanticCachePort durable = mock(SemanticCachePort.class);
    UUID tenantId = UUID.randomUUID();
    UUID databaseId = UUID.randomUUID();
    AiTenantContextResolver contextResolver = mock(AiTenantContextResolver.class);
    org.mockito.Mockito.when(
            contextResolver.resolve(
                org.mockito.ArgumentMatchers.eq(tenantId),
                org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(new TenantExecutionContext(tenantId, databaseId, "correlation"));
    org.mockito.Mockito.doAnswer(
            invocation -> {
              assertThat(TenantContextHolder.currentDatabaseOptional()).contains(databaseId);
              return null;
            })
        .when(durable)
        .invalidate(any(SemanticCacheInvalidation.class));
    SemanticCacheInvalidationService service =
        new SemanticCacheInvalidationService(
            durable,
            java.util.Optional.empty(),
            mock(SemanticMetrics.class),
            mock(AiTraceRecorder.class),
            java.util.Optional.of(contextResolver));

    service.invalidate(tenantWideEvent(tenantId));

    verify(durable).invalidate(any(SemanticCacheInvalidation.class));
  }

  @Test
  void resolvesNullTenantDatabaseToTheConfiguredDefaultDatabase() {
    SemanticCachePort durable = mock(SemanticCachePort.class);
    UUID tenantId = UUID.randomUUID();
    UUID defaultDatabaseId = UUID.randomUUID();
    AiTenantContextResolver contextResolver = mock(AiTenantContextResolver.class);
    org.mockito.Mockito.when(
            contextResolver.resolve(
                org.mockito.ArgumentMatchers.eq(tenantId),
                org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(new TenantExecutionContext(tenantId, defaultDatabaseId, "correlation"));
    org.mockito.Mockito.doAnswer(
            invocation -> {
              assertThat(TenantContextHolder.currentDatabaseOptional()).contains(defaultDatabaseId);
              return null;
            })
        .when(durable)
        .invalidate(any(SemanticCacheInvalidation.class));
    SemanticCacheInvalidationService service =
        new SemanticCacheInvalidationService(
            durable,
            java.util.Optional.empty(),
            mock(SemanticMetrics.class),
            mock(AiTraceRecorder.class),
            java.util.Optional.of(contextResolver));

    service.invalidate(tenantWideEvent(tenantId));

    verify(durable).invalidate(any(SemanticCacheInvalidation.class));
  }

  @Test
  void failsClosedWhenTheConfiguredDefaultDatabaseIsInvalid() {
    SemanticCachePort durable = mock(SemanticCachePort.class);
    UUID tenantId = UUID.randomUUID();
    AiTenantContextResolver contextResolver = mock(AiTenantContextResolver.class);
    org.mockito.Mockito.when(
            contextResolver.resolve(
                org.mockito.ArgumentMatchers.eq(tenantId),
                org.mockito.ArgumentMatchers.anyString()))
        .thenThrow(
            new IllegalStateException("No valid default database for semantic cache invalidation"));
    SemanticCacheInvalidationService service =
        new SemanticCacheInvalidationService(
            durable,
            java.util.Optional.empty(),
            mock(SemanticMetrics.class),
            mock(AiTraceRecorder.class),
            java.util.Optional.of(contextResolver));

    assertThatThrownBy(() -> service.invalidate(tenantWideEvent(tenantId)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No valid default database for semantic cache invalidation");
    org.mockito.Mockito.verifyNoInteractions(durable);
  }

  @Test
  void failsClosedWhenTheTenantDatabaseCannotBeResolved() {
    SemanticCachePort durable = mock(SemanticCachePort.class);
    UUID tenantId = UUID.randomUUID();
    SemanticCacheInvalidationService service =
        new SemanticCacheInvalidationService(
            durable,
            java.util.Optional.empty(),
            mock(SemanticMetrics.class),
            mock(AiTraceRecorder.class));

    assertThatThrownBy(() -> service.invalidate(tenantWideEvent(tenantId)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No valid default database for semantic cache invalidation");
    org.mockito.Mockito.verifyNoInteractions(durable);
  }

  @Test
  void rejectsAnEventThatDoesNotMatchAnAlreadyBoundBackendTenant() {
    SemanticCachePort durable = mock(SemanticCachePort.class);
    SemanticCacheInvalidationService service =
        new SemanticCacheInvalidationService(
            durable,
            java.util.Optional.empty(),
            mock(SemanticMetrics.class),
            mock(AiTraceRecorder.class),
            java.util.Optional.of(tenantContextResolver()));
    UUID boundTenant = UUID.randomUUID();
    SemanticCacheDependencyChanged event = tenantWideEvent(UUID.randomUUID());
    AiExecutionContext context =
        new AiExecutionContext(
            boundTenant,
            UUID.randomUUID(),
            Set.of("ROLE_SYSTEM"),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "trace",
            "idempotency");

    assertThat(
            org.assertj.core.api.Assertions.catchThrowable(
                () -> AiExecutionContextScope.run(context, () -> service.invalidate(event))))
        .isInstanceOf(SecurityException.class)
        .hasMessage("Semantic invalidation tenant does not match backend context");
    org.mockito.Mockito.verifyNoInteractions(durable);
  }

  @Test
  void keepsDurableInvalidationWhenTheRedisProjectionIsUnavailable() {
    SemanticCachePort durable = mock(SemanticCachePort.class);
    SemanticCacheHotStore hot = mock(SemanticCacheHotStore.class);
    SemanticMetrics metrics = mock(SemanticMetrics.class);
    org.mockito.Mockito.doThrow(new IllegalStateException("redis unavailable"))
        .when(hot)
        .invalidate(any());
    SemanticCacheInvalidationService service =
        new SemanticCacheInvalidationService(
            durable,
            java.util.Optional.of(hot),
            metrics,
            mock(AiTraceRecorder.class),
            java.util.Optional.of(tenantContextResolver()));
    SemanticCacheDependencyChanged event =
        new SemanticCacheDependencyChanged(
            UUID.randomUUID(),
            UUID.randomUUID(),
            null,
            SemanticCacheDependencyChanged.Dependency.TENANT_POLICY,
            "policy-v4",
            Instant.parse("2026-08-31T12:00:00Z"));

    service.invalidate(event);

    verify(durable).invalidate(any(SemanticCacheInvalidation.class));
    verify(metrics).recordFallback("invalidation", "hot_store_unavailable");
    assertThat(event.principalId()).isNull();
  }

  @Test
  void recordsTheDependencyAndScopeForInvalidation() {
    SemanticCachePort durable = mock(SemanticCachePort.class);
    SemanticMetrics metrics = mock(SemanticMetrics.class);
    SemanticCacheInvalidationService service =
        new SemanticCacheInvalidationService(
            durable,
            java.util.Optional.empty(),
            metrics,
            mock(AiTraceRecorder.class),
            java.util.Optional.of(tenantContextResolver()));
    SemanticCacheDependencyChanged event =
        new SemanticCacheDependencyChanged(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            SemanticCacheDependencyChanged.Dependency.QUOTE_TEMPLATE,
            "template-v3",
            Instant.parse("2026-08-31T12:00:00Z"));

    service.invalidate(event);

    verify(metrics).recordInvalidation("QUOTE_TEMPLATE", "principal");
  }

  @Test
  void recordsTenantScopedInvalidationContextThroughTheDurableTraceBoundary() {
    SemanticCachePort durable = mock(SemanticCachePort.class);
    AiTraceRecorder traces = mock(AiTraceRecorder.class);
    SemanticCacheInvalidationService service =
        new SemanticCacheInvalidationService(
            durable,
            java.util.Optional.empty(),
            mock(SemanticMetrics.class),
            traces,
            java.util.Optional.of(tenantContextResolver()));
    SemanticCacheDependencyChanged event =
        new SemanticCacheDependencyChanged(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            SemanticCacheDependencyChanged.Dependency.QUOTE_TEMPLATE,
            "template-v3",
            Instant.parse("2026-08-31T12:00:00Z"));

    service.invalidate(event);

    var trace = org.mockito.ArgumentCaptor.forClass(AiSemanticExecutionTrace.class);
    verify(traces).recordSemanticOutcome(trace.capture());
    assertThat(trace.getValue().tenantId()).isEqualTo(event.tenantId());
    assertThat(trace.getValue().principalId()).isEqualTo(event.principalId());
    assertThat(trace.getValue().outcome()).isEqualTo("completed");
    assertThat(trace.getValue().dependency()).isEqualTo("QUOTE_TEMPLATE");
    assertThat(trace.getValue().dependencyVersion()).isEqualTo("template-v3");
  }

  @Test
  void reportsDurableInvalidationFailureAndDoesNotClaimHotInvalidation() {
    SemanticCachePort durable = mock(SemanticCachePort.class);
    SemanticCacheHotStore hot = mock(SemanticCacheHotStore.class);
    SemanticMetrics metrics = mock(SemanticMetrics.class);
    AiTraceRecorder traces = mock(AiTraceRecorder.class);
    org.mockito.Mockito.doThrow(new IllegalStateException("database unavailable"))
        .when(durable)
        .invalidate(any(SemanticCacheInvalidation.class));
    SemanticCacheInvalidationService service =
        new SemanticCacheInvalidationService(
            durable,
            java.util.Optional.of(hot),
            metrics,
            traces,
            java.util.Optional.of(tenantContextResolver()));
    SemanticCacheDependencyChanged event =
        new SemanticCacheDependencyChanged(
            UUID.randomUUID(),
            UUID.randomUUID(),
            null,
            SemanticCacheDependencyChanged.Dependency.PRICE,
            "price-v2",
            Instant.parse("2026-08-31T12:00:00Z"));

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.invalidate(event))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("database unavailable");

    verify(metrics).recordFailure("invalidation", "durable_store_unavailable");
    org.mockito.Mockito.verifyNoInteractions(hot);
    var trace = org.mockito.ArgumentCaptor.forClass(AiSemanticExecutionTrace.class);
    verify(traces).recordSemanticOutcome(trace.capture());
    assertThat(trace.getValue().tenantId()).isEqualTo(event.tenantId());
    assertThat(trace.getValue().outcome()).isEqualTo("failed");
  }

  private static SemanticCacheDependencyChanged tenantWideEvent(UUID tenantId) {
    return new SemanticCacheDependencyChanged(
        UUID.randomUUID(),
        tenantId,
        null,
        SemanticCacheDependencyChanged.Dependency.PRICE,
        "price-v2",
        Instant.parse("2026-08-31T12:00:00Z"));
  }

  private static AiTenantContextResolver tenantContextResolver() {
    AiTenantContextResolver resolver = mock(AiTenantContextResolver.class);
    org.mockito.Mockito.when(
            resolver.resolve(any(UUID.class), org.mockito.ArgumentMatchers.anyString()))
        .thenAnswer(
            invocation ->
                new TenantExecutionContext(
                    invocation.getArgument(0), UUID.randomUUID(), invocation.getArgument(1)));
    return resolver;
  }
}
