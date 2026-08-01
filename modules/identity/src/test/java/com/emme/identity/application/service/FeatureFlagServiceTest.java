package com.emme.identity.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emme.identity.api.command.SetPlatformFeatureFlagCommand;
import com.emme.identity.api.query.GetEffectiveFeatureFlagsQuery;
import com.emme.identity.application.port.out.FeatureFlagRepository;
import com.emme.identity.domain.model.FeatureFlag;
import com.emme.studio.subscriptions.api.PlanType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FeatureFlagServiceTest {

  @Test
  void overlaysTenantOverridesOnGlobalDefaults() {
    UUID tenantId = UUID.randomUUID();
    InMemoryFeatureFlagRepository repository = new InMemoryFeatureFlagRepository();
    repository.flags.add(new FeatureFlag(null, "calendar_sync", false, null, "Global default"));
    repository.flags.add(new FeatureFlag(tenantId, "calendar_sync", true, null, "Tenant override"));
    repository.flags.add(new FeatureFlag(null, "ai_chat", true, null, "Global default"));

    FeatureFlagService service = new FeatureFlagService(repository, ignored -> Optional.empty());

    assertThat(service.getEffective(tenantId))
        .containsEntry("calendar_sync", true)
        .containsEntry("ai_chat", true);
  }

  @Test
  void createsTenantOverrideWithTheRequestedState() {
    UUID tenantId = UUID.randomUUID();
    InMemoryFeatureFlagRepository repository = new InMemoryFeatureFlagRepository();
    FeatureFlagService service = new FeatureFlagService(repository, ignored -> Optional.empty());

    FeatureFlag result = service.setOverride(tenantId, "calendar_sync", true);

    assertThat(result.tenantId()).isEqualTo(tenantId);
    assertThat(result.code()).isEqualTo("calendar_sync");
    assertThat(result.isEnabled()).isTrue();
    assertThat(result.description()).isEqualTo("Tenant override");
  }

  @Test
  void createsGlobalFlagWithItsPlanRequirement() {
    InMemoryFeatureFlagRepository repository = new InMemoryFeatureFlagRepository();
    FeatureFlagService service = new FeatureFlagService(repository, ignored -> Optional.empty());

    FeatureFlag result = service.platformSet("calendar_sync", true, PlanType.ENTERPRISE);

    assertThat(result.tenantId()).isNull();
    assertThat(result.planRequired()).isEqualTo(PlanType.ENTERPRISE);
  }

  @Test
  void exposesGlobalFlagAsPublicResultThroughTheUseCase() {
    InMemoryFeatureFlagRepository repository = new InMemoryFeatureFlagRepository();
    FeatureFlagService service = new FeatureFlagService(repository, ignored -> Optional.empty());

    var result =
        service.set(new SetPlatformFeatureFlagCommand("calendar_sync", true, PlanType.PRO));

    assertThat(result.code()).isEqualTo("calendar_sync");
    assertThat(result.enabled()).isTrue();
    assertThat(result.planRequired()).isEqualTo(PlanType.PRO);
  }

  @Test
  void exposesEffectiveFlagsAsAnImmutablePublicResult() {
    UUID tenantId = UUID.randomUUID();
    InMemoryFeatureFlagRepository repository = new InMemoryFeatureFlagRepository();
    repository.flags.add(new FeatureFlag(null, "calendar_sync", false, null, "Global default"));
    repository.flags.add(new FeatureFlag(tenantId, "calendar_sync", true, null, "Tenant override"));
    FeatureFlagService service = new FeatureFlagService(repository, ignored -> Optional.empty());

    var result = service.get(new GetEffectiveFeatureFlagsQuery(tenantId));

    assertThat(result.values()).containsEntry("calendar_sync", true);
    assertThatThrownBy(() -> result.values().put("new_flag", true))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  private static final class InMemoryFeatureFlagRepository implements FeatureFlagRepository {

    private final List<FeatureFlag> flags = new ArrayList<>();

    @Override
    public Optional<FeatureFlag> findTenantOverride(UUID tenantId, String code) {
      return flags.stream()
          .filter(flag -> tenantId.equals(flag.tenantId()) && code.equals(flag.code()))
          .findFirst();
    }

    @Override
    public List<FeatureFlag> findGlobalDefaults() {
      return flags.stream().filter(flag -> flag.tenantId() == null).toList();
    }

    @Override
    public List<FeatureFlag> findByTenantOrGlobal(UUID tenantId) {
      return flags.stream()
          .filter(flag -> flag.tenantId() == null || tenantId.equals(flag.tenantId()))
          .toList();
    }

    @Override
    public FeatureFlag save(FeatureFlag flag) {
      flags.removeIf(
          current ->
              current.tenantId() == null
                  ? flag.tenantId() == null && current.code().equals(flag.code())
                  : current.tenantId().equals(flag.tenantId())
                      && current.code().equals(flag.code()));
      flags.add(flag);
      return flag;
    }
  }
}
