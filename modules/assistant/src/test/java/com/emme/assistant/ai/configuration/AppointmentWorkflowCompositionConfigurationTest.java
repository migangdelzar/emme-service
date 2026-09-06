package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.emme.ai.contracts.workflow.PaymentWorkflow;
import com.emme.appointments.api.usecase.ConfirmAppointmentUseCase;
import com.emme.appointments.api.usecase.CreateAppointmentHoldUseCase;
import com.emme.appointments.application.port.out.AppointmentHoldRepository;
import com.emme.appointments.application.port.out.AppointmentRepository;
import com.emme.assistant.ai.adapter.out.workflow.AppointmentBookingWorkflow;
import com.emme.assistant.ai.adapter.out.workflow.AppointmentPaymentWorkflow;
import com.emme.assistant.ai.application.port.out.PaymentWorkflowCheckpointRepository;
import com.emme.payment.api.usecase.CreatePaymentLinkUseCase;
import com.emme.payment.application.port.out.PaymentLinkRepository;
import com.emme.payment.application.port.out.PaymentLinkSourceRepository;
import com.emme.payment.application.port.out.PaymentProvider;
import com.emme.payment.application.port.out.PaymentWorkflowCorrelationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AppointmentWorkflowCompositionConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withUserConfiguration(AppointmentWorkflowCompositionConfiguration.class)
          .withBean(AppointmentRepository.class, () -> mock(AppointmentRepository.class))
          .withBean(AppointmentHoldRepository.class, () -> mock(AppointmentHoldRepository.class))
          .withBean(PaymentLinkRepository.class, () -> mock(PaymentLinkRepository.class))
          .withBean(
              PaymentLinkSourceRepository.class, () -> mock(PaymentLinkSourceRepository.class))
          .withBean(PaymentProvider.class, () -> mock(PaymentProvider.class))
          .withBean(
              PaymentWorkflowCorrelationRepository.class,
              () -> mock(PaymentWorkflowCorrelationRepository.class))
          .withBean(ConfirmAppointmentUseCase.class, () -> mock(ConfirmAppointmentUseCase.class))
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
            });
  }
}
