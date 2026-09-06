package com.emme.assistant.ai.configuration;

import com.emme.ai.contracts.workflow.PaymentWorkflow;
import com.emme.appointments.api.usecase.ConfirmAppointmentUseCase;
import com.emme.appointments.api.usecase.CreateAppointmentHoldUseCase;
import com.emme.appointments.application.port.out.AppointmentHoldRepository;
import com.emme.appointments.application.port.out.AppointmentRepository;
import com.emme.appointments.application.service.CreateAppointmentHoldService;
import com.emme.assistant.ai.adapter.out.persistence.PaymentWorkflowAppointmentRepositoryAdapter;
import com.emme.assistant.ai.adapter.out.workflow.AppointmentBookingWorkflow;
import com.emme.assistant.ai.adapter.out.workflow.AppointmentPaymentWorkflow;
import com.emme.assistant.ai.application.port.out.PaymentWorkflowAppointmentRepository;
import com.emme.payment.api.usecase.CreatePaymentLinkUseCase;
import com.emme.payment.application.port.out.PaymentLinkRepository;
import com.emme.payment.application.port.out.PaymentLinkSourceRepository;
import com.emme.payment.application.port.out.PaymentProvider;
import com.emme.payment.application.port.out.PaymentWorkflowCorrelationRepository;
import com.emme.payment.application.service.CreatePaymentLinkService;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composition root for the hold-first appointment/payment workflow boundary. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AppointmentWorkflowProperties.class)
@ConditionalOnProperty(prefix = "app.ai.langgraph", name = "enabled", havingValue = "true")
public class AppointmentWorkflowCompositionConfiguration {

  @Bean(name = "appointmentWorkflowClock")
  Clock appointmentWorkflowClock() {
    return Clock.systemUTC();
  }

  @Bean
  CreateAppointmentHoldUseCase appointmentHoldUseCase(
      AppointmentRepository appointments,
      AppointmentHoldRepository holds,
      AppointmentWorkflowProperties properties,
      @Qualifier("appointmentWorkflowClock") Clock appointmentWorkflowClock) {
    return new CreateAppointmentHoldService(
        appointments, holds, appointmentWorkflowClock, properties.holdDuration());
  }

  @Bean
  CreatePaymentLinkUseCase paymentLinkUseCase(
      PaymentLinkRepository links,
      PaymentLinkSourceRepository sources,
      PaymentProvider provider,
      PaymentWorkflowCorrelationRepository correlations) {
    return new CreatePaymentLinkService(links, sources, provider, correlations);
  }

  @Bean
  PaymentWorkflowAppointmentRepository paymentWorkflowAppointmentRepository(
      PaymentWorkflowCorrelationRepository correlations,
      AppointmentHoldRepository holds,
      @Qualifier("appointmentWorkflowClock") Clock appointmentWorkflowClock) {
    return new PaymentWorkflowAppointmentRepositoryAdapter(
        correlations, holds, appointmentWorkflowClock);
  }

  @Bean
  AppointmentBookingWorkflow appointmentBookingWorkflow(
      CreateAppointmentHoldUseCase holds, CreatePaymentLinkUseCase paymentLinks) {
    return new AppointmentBookingWorkflow(holds, paymentLinks);
  }

  @Bean
  PaymentWorkflow appointmentPaymentWorkflow(
      ConfirmAppointmentUseCase confirmations, PaymentWorkflowAppointmentRepository appointments) {
    return new AppointmentPaymentWorkflow(confirmations, appointments);
  }
}
