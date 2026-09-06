package com.emme.payment.configuration;

import com.emme.payment.api.port.out.PaymentLinkSourceRepository;
import com.emme.payment.api.port.out.PaymentWorkflowCorrelationRepository;
import com.emme.payment.api.usecase.CreatePaymentLinkUseCase;
import com.emme.payment.application.port.out.PaymentLinkRepository;
import com.emme.payment.application.port.out.PaymentProvider;
import com.emme.payment.application.service.CreatePaymentLinkService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composes payment-link creation from public workflow input and payment-owned persistence. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.ai.langgraph", name = "enabled", havingValue = "true")
@ConditionalOnBean(PaymentLinkSourceRepository.class)
class PaymentWorkflowConfiguration {

  @Bean
  CreatePaymentLinkUseCase paymentWorkflowLinkUseCase(
      PaymentLinkRepository links,
      PaymentLinkSourceRepository sources,
      PaymentProvider provider,
      PaymentWorkflowCorrelationRepository correlations) {
    return new CreatePaymentLinkService(links, sources, provider, correlations);
  }
}
