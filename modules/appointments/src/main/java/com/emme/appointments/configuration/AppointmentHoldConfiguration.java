package com.emme.appointments.configuration;

import com.emme.appointments.api.usecase.CreateAppointmentHoldUseCase;
import com.emme.appointments.application.port.out.AppointmentHoldRepository;
import com.emme.appointments.application.port.out.AppointmentRepository;
import com.emme.appointments.application.service.CreateAppointmentHoldService;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composition root for the public appointment-hold application boundary. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AppointmentHoldProperties.class)
public class AppointmentHoldConfiguration {

  @Bean(name = "appointmentHoldClock")
  Clock appointmentHoldClock() {
    return Clock.systemUTC();
  }

  @Bean
  CreateAppointmentHoldUseCase createAppointmentHoldUseCase(
      AppointmentRepository appointments,
      AppointmentHoldRepository holds,
      AppointmentHoldProperties properties,
      @Qualifier("appointmentHoldClock") Clock clock) {
    return new CreateAppointmentHoldService(appointments, holds, clock, properties.holdDuration());
  }
}
