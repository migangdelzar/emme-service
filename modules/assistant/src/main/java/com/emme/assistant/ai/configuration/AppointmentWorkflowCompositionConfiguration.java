package com.emme.assistant.ai.configuration;

import com.emme.ai.contracts.workflow.PaymentWorkflow;
import com.emme.appointments.api.usecase.CancelAuthorizedAppointmentUseCase;
import com.emme.appointments.api.usecase.ConfirmAppointmentUseCase;
import com.emme.appointments.api.usecase.CreateAppointmentHoldUseCase;
import com.emme.appointments.api.usecase.RescheduleAuthorizedAppointmentUseCase;
import com.emme.appointments.application.port.out.AppointmentHoldRepository;
import com.emme.appointments.application.port.out.AppointmentRepository;
import com.emme.appointments.application.service.CreateAppointmentHoldService;
import com.emme.assistant.ai.adapter.out.persistence.PaymentWorkflowAppointmentRepositoryAdapter;
import com.emme.assistant.ai.adapter.out.workflow.AppointmentBookingWorkflow;
import com.emme.assistant.ai.adapter.out.workflow.AppointmentCancellationWorkflow;
import com.emme.assistant.ai.adapter.out.workflow.AppointmentPaymentWorkflow;
import com.emme.assistant.ai.adapter.out.workflow.AppointmentRescheduleWorkflow;
import com.emme.assistant.ai.application.port.out.PaymentWorkflowAppointmentRepository;
import com.emme.assistant.ai.application.port.out.PaymentWorkflowCheckpointRepository;
import com.emme.assistant.ai.application.port.out.WorkflowCheckpointRepository;
import com.emme.payment.api.port.out.PaymentWorkflowCorrelationRepository;
import com.emme.payment.api.usecase.CreatePaymentLinkUseCase;
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
  PaymentWorkflowAppointmentRepository paymentWorkflowAppointmentRepository(
      PaymentWorkflowCorrelationRepository correlations,
      AppointmentHoldRepository holds,
      @Qualifier("appointmentWorkflowClock") Clock appointmentWorkflowClock) {
    return new PaymentWorkflowAppointmentRepositoryAdapter(
        correlations, holds, appointmentWorkflowClock);
  }

  @Bean
  AppointmentBookingWorkflow appointmentBookingWorkflow(
      CreateAppointmentHoldUseCase holds,
      CreatePaymentLinkUseCase paymentLinks,
      PaymentWorkflowCheckpointRepository checkpoints) {
    return new AppointmentBookingWorkflow(holds, paymentLinks, checkpoints);
  }

  @Bean
  PaymentWorkflow appointmentPaymentWorkflow(
      ConfirmAppointmentUseCase confirmations,
      PaymentWorkflowAppointmentRepository appointments,
      PaymentWorkflowCheckpointRepository checkpoints) {
    return new AppointmentPaymentWorkflow(confirmations, appointments, checkpoints);
  }

  @Bean
  AppointmentRescheduleWorkflow appointmentRescheduleWorkflow(
      RescheduleAuthorizedAppointmentUseCase rescheduling,
      WorkflowCheckpointRepository checkpoints) {
    return new AppointmentRescheduleWorkflow(rescheduling, checkpoints);
  }

  @Bean
  AppointmentCancellationWorkflow appointmentCancellationWorkflow(
      CancelAuthorizedAppointmentUseCase cancellation, WorkflowCheckpointRepository checkpoints) {
    return new AppointmentCancellationWorkflow(cancellation, checkpoints);
  }
}
