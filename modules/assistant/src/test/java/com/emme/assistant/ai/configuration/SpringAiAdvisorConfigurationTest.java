package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.adapter.out.provider.springai.advisor.PromptVersionAdvisor;
import com.emme.assistant.ai.adapter.out.provider.springai.advisor.TenantSecurityAdvisor;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.advisor.api.Advisor;

class SpringAiAdvisorConfigurationTest {

  @Test
  void createsTheSecurityAndPromptAdvisorsForModelClients() {
    SpringAiAdvisorConfiguration configuration = new SpringAiAdvisorConfiguration();

    assertThat(configuration.tenantSecurityAdvisor()).isInstanceOf(TenantSecurityAdvisor.class);
    assertThat(configuration.promptVersionAdvisor()).isInstanceOf(PromptVersionAdvisor.class);
  }

  @Test
  void ordersTheAdvisorChainBySpringAiPrecedenceInsteadOfAssemblyOrder() {
    SpringAiAdvisorConfiguration configuration = new SpringAiAdvisorConfiguration();
    TenantSecurityAdvisor security = configuration.tenantSecurityAdvisor();
    PromptVersionAdvisor prompt = configuration.promptVersionAdvisor();
    Advisor retrieval = mock(Advisor.class);
    when(retrieval.getOrder()).thenReturn(0);

    assertThat(SpringAiAdvisorConfiguration.orderedAdvisors(List.of(retrieval, prompt, security)))
        .containsExactly(security, prompt, retrieval);
  }
}
