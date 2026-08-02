package com.emme.identity.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.identity.domain.model.FeatureFlag;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListPlatformFeatureFlagsServiceTest {

  @Test
  void returnsAllGlobalFeatureFlagsAsPublicResults() {
    FeatureFlagTestRepository repository = new FeatureFlagTestRepository();
    repository.flags.add(new FeatureFlag(null, "calendar_sync", true, null, "Calendar"));
    repository.flags.add(new FeatureFlag(null, "ai_chat", false, null, "AI"));
    repository.flags.add(
        new FeatureFlag(UUID.randomUUID(), "tenant_only", true, null, "Tenant override"));

    ListPlatformFeatureFlagsService service = new ListPlatformFeatureFlagsService(repository);

    assertThat(service.list())
        .extracting(result -> result.code())
        .containsExactly("calendar_sync", "ai_chat");
  }
}
