package com.emme.appointments.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.emme.appointments.api.usecase.CreateAppointmentHoldUseCase;
import com.emme.appointments.application.port.out.AppointmentHoldRepository;
import com.emme.appointments.application.port.out.AppointmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AppointmentHoldConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withUserConfiguration(AppointmentHoldConfiguration.class)
          .withBean(AppointmentRepository.class, () -> mock(AppointmentRepository.class))
          .withBean(AppointmentHoldRepository.class, () -> mock(AppointmentHoldRepository.class));

  @Test
  void exposesTheAppointmentHoldUseCaseFromTheAppointmentsModule() {
    contextRunner
        .withPropertyValues("app.ai.appointment-workflow.hold-duration=15m")
        .run(context -> assertThat(context).hasSingleBean(CreateAppointmentHoldUseCase.class));
  }
}
