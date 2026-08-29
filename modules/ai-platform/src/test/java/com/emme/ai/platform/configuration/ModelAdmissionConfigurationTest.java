package com.emme.ai.platform.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emme.ai.platform.model.BoundedModelExecutionScheduler;
import org.junit.jupiter.api.Test;

class ModelAdmissionConfigurationTest {

  @Test
  void createsTheExistingBoundedSchedulerFromApplicationConfiguration() {
    var properties = new ModelAdmissionProperties(2, 1, 2, 1, 1, 8);
    var configuration = new AiProviderConfiguration();

    assertThat(configuration.modelExecutionScheduler(properties))
        .isInstanceOf(BoundedModelExecutionScheduler.class);
  }

  @Test
  void rejectsUnsafeCapacityConfiguration() {
    assertThatThrownBy(() -> new ModelAdmissionProperties(0, 1, 2, 1, 1, 8))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("globalLimit must be greater than zero");

    assertThatThrownBy(() -> new ModelAdmissionProperties(2, 1, 2, 1, 1, -1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("queueCapacity must not be negative");
  }
}
