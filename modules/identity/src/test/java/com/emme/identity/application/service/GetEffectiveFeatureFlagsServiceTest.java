package com.emme.identity.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emme.identity.api.query.GetEffectiveFeatureFlagsQuery;
import com.emme.identity.application.authorization.FeatureFlagEvaluator;
import com.emme.identity.application.support.FeatureFlagTestRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetEffectiveFeatureFlagsServiceTest {

  @Test
  void exposesEffectiveFlagsAsAnImmutablePublicResult() {
    UUID tenantId = UUID.randomUUID();
    FeatureFlagTestRepository repository = new FeatureFlagTestRepository();
    repository.addGlobal("calendar_sync", false);
    repository.addTenantOverride(tenantId, "calendar_sync", true);
    FeatureFlagEvaluator evaluator =
        new FeatureFlagEvaluator(repository, ignored -> java.util.Optional.empty());
    GetEffectiveFeatureFlagsService service = new GetEffectiveFeatureFlagsService(evaluator);

    var result = service.get(new GetEffectiveFeatureFlagsQuery(tenantId));

    assertThat(result.values()).containsEntry("calendar_sync", true);
    assertThatThrownBy(() -> result.values().put("new_flag", true))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
