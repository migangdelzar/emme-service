package com.emme.identity.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.emme.ai.contracts.semantic.SemanticCacheDependencyChanged;
import com.emme.ai.contracts.semantic.SemanticCacheDependencyPublisher;
import com.emme.identity.api.command.SetTenantFeatureFlagOverrideCommand;
import com.emme.identity.application.port.out.FeatureFlagRepository;
import com.emme.identity.application.support.FeatureFlagTestRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SetTenantFeatureFlagOverrideServiceTest {

  @Test
  void marksTheDependencyAwareConstructorAsTheSpringInjectionConstructor() throws Exception {
    assertThat(
            SetTenantFeatureFlagOverrideService.class
                .getConstructor(FeatureFlagRepository.class, Optional.class)
                .isAnnotationPresent(Autowired.class))
        .isTrue();
  }

  @Test
  void createsTenantOverrideWithTheRequestedState() {
    UUID tenantId = UUID.randomUUID();
    SetTenantFeatureFlagOverrideService service =
        new SetTenantFeatureFlagOverrideService(new FeatureFlagTestRepository());

    var result =
        service.set(new SetTenantFeatureFlagOverrideCommand(tenantId, "calendar_sync", true));

    assertThat(result.code()).isEqualTo("calendar_sync");
    assertThat(result.enabled()).isTrue();
  }

  @Test
  void publishesTenantPolicyInvalidationAfterAnOverrideChanges() {
    UUID tenantId = UUID.randomUUID();
    SemanticCacheDependencyPublisher publisher = mock(SemanticCacheDependencyPublisher.class);
    SetTenantFeatureFlagOverrideService service =
        new SetTenantFeatureFlagOverrideService(
            new FeatureFlagTestRepository(), Optional.of(publisher));

    service.set(new SetTenantFeatureFlagOverrideCommand(tenantId, "calendar_sync", true));

    org.mockito.ArgumentCaptor<SemanticCacheDependencyChanged> event =
        org.mockito.ArgumentCaptor.forClass(SemanticCacheDependencyChanged.class);
    verify(publisher).publish(event.capture());
    assertThat(event.getValue().tenantId()).isEqualTo(tenantId);
    assertThat(event.getValue().principalId()).isNull();
    assertThat(event.getValue().dependency())
        .isEqualTo(SemanticCacheDependencyChanged.Dependency.TENANT_POLICY);
  }
}
