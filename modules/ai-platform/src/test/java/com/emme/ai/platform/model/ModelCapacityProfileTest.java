package com.emme.ai.platform.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.ai.contracts.model.ModelCapability;
import org.junit.jupiter.api.Test;

class ModelCapacityProfileTest {

  @Test
  void exposesTheCapacityForEachModelCapability() {
    var profile = new ModelCapacityProfile(4, 2, 3, 1, 1, 8);

    assertThat(profile.limitFor(ModelCapability.GENERATION)).isEqualTo(2);
    assertThat(profile.limitFor(ModelCapability.VISION)).isEqualTo(2);
    assertThat(profile.limitFor(ModelCapability.EMBEDDING)).isEqualTo(3);
  }
}
