package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.emme.ai.contracts.workflow.PaymentWorkflow;
import com.emme.appointments.api.usecase.CancelAuthorizedAppointmentUseCase;
import com.emme.appointments.api.usecase.ConfirmAppointmentUseCase;
import com.emme.appointments.api.usecase.CreateAppointmentHoldUseCase;
import com.emme.appointments.api.usecase.GetAppointmentHoldUseCase;
import com.emme.appointments.api.usecase.RescheduleAuthorizedAppointmentUseCase;
import com.emme.assistant.ai.adapter.out.workflow.AppointmentBookingWorkflow;
import com.emme.assistant.ai.adapter.out.workflow.AppointmentCancellationWorkflow;
import com.emme.assistant.ai.adapter.out.workflow.AppointmentPaymentWorkflow;
import com.emme.assistant.ai.adapter.out.workflow.AppointmentRescheduleWorkflow;
import com.emme.assistant.ai.application.port.out.PaymentWorkflowCheckpointRepository;
import com.emme.payment.api.port.out.PaymentLinkSourceRepository;
import com.emme.payment.api.port.out.PaymentWorkflowCorrelationRepository;
import com.emme.payment.api.usecase.CreatePaymentLinkUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AppointmentWorkflowCompositionConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withUserConfiguration(AppointmentWorkflowCompositionConfiguration.class)
          .withBean(
              CreateAppointmentHoldUseCase.class, () -> mock(CreateAppointmentHoldUseCase.class))
          .withBean(GetAppointmentHoldUseCase.class, () -> mock(GetAppointmentHoldUseCase.class))
          .withBean(CreatePaymentLinkUseCase.class, () -> mock(CreatePaymentLinkUseCase.class))
          .withBean(
              PaymentLinkSourceRepository.class, () -> mock(PaymentLinkSourceRepository.class))
          .withBean(
              PaymentWorkflowCorrelationRepository.class,
              () -> mock(PaymentWorkflowCorrelationRepository.class))
          .withBean(ConfirmAppointmentUseCase.class, () -> mock(ConfirmAppointmentUseCase.class))
          .withBean(
              RescheduleAuthorizedAppointmentUseCase.class,
              () -> mock(RescheduleAuthorizedAppointmentUseCase.class))
          .withBean(
              CancelAuthorizedAppointmentUseCase.class,
              () -> mock(CancelAuthorizedAppointmentUseCase.class))
          .withBean(
              PaymentWorkflowCheckpointRepository.class,
              () -> mock(PaymentWorkflowCheckpointRepository.class));

  @Test
  void composesAppointmentAndPaymentWorkflowsOnlyWhenLangGraphIsEnabled() {
    contextRunner
        .withPropertyValues("app.ai.langgraph.enabled=true")
        .run(
            context -> {
              assertThat(context).hasSingleBean(CreateAppointmentHoldUseCase.class);
              assertThat(context).hasSingleBean(CreatePaymentLinkUseCase.class);
              assertThat(context).hasSingleBean(AppointmentBookingWorkflow.class);
              assertThat(context).hasSingleBean(AppointmentPaymentWorkflow.class);
              assertThat(context).hasSingleBean(AppointmentRescheduleWorkflow.class);
              assertThat(context).hasSingleBean(AppointmentCancellationWorkflow.class);
              assertThat(context).hasSingleBean(PaymentWorkflow.class);
            });
  }

  @Test
  void doesNotCreateWorkflowServicesWhenLangGraphIsDisabled() {
    contextRunner
        .withPropertyValues("app.ai.langgraph.enabled=false")
        .run(
            context -> {
              assertThat(context).doesNotHaveBean(AppointmentBookingWorkflow.class);
              assertThat(context).doesNotHaveBean(AppointmentPaymentWorkflow.class);
              assertThat(context).doesNotHaveBean(AppointmentRescheduleWorkflow.class);
              assertThat(context).doesNotHaveBean(AppointmentCancellationWorkflow.class);
            });
  }
}
