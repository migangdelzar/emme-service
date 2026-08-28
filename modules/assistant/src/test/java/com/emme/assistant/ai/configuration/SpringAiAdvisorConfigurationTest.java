package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.assistant.ai.adapter.out.provider.springai.advisor.PromptVersionAdvisor;
import com.emme.assistant.ai.adapter.out.provider.springai.advisor.TenantSecurityAdvisor;
import org.junit.jupiter.api.Test;

class SpringAiAdvisorConfigurationTest {

  @Test
  void createsTheSecurityAndPromptAdvisorsForModelClients() {
    SpringAiAdvisorConfiguration configuration = new SpringAiAdvisorConfiguration();

    assertThat(configuration.tenantSecurityAdvisor()).isInstanceOf(TenantSecurityAdvisor.class);
    assertThat(configuration.promptVersionAdvisor()).isInstanceOf(PromptVersionAdvisor.class);
  }
}
