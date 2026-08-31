package com.emme.assistant.ai.adapter.out.tool;

import com.emme.appointments.api.usecase.*;
import com.emme.assistant.ai.application.tool.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.*;

@Configuration(proxyBeanMethods = false)
@ConditionalOnBean({
  BookAppointmentUseCase.class,
  CancelAuthorizedAppointmentUseCase.class,
  RescheduleAuthorizedAppointmentUseCase.class
})
public class AppointmentToolConfiguration {
  @Bean
  AiToolDefinition createAppointmentTool(BookAppointmentUseCase u, ObjectMapper m) {
    return new AiToolDefinition(
        "createAppointment",
        "Book an appointment",
        Set.of("client", "tenant_staff", "tenant_owner", "admin"),
        AiToolRisk.MUTATION,
        true,
        false,
        new CreateAppointmentToolHandler(u, m),
        Set.of("customerId", "serviceId", "artistId", "startsAt", "endsAt"),
        Set.of("customerId", "serviceId", "artistId", "startsAt", "endsAt"));
  }

  @Bean
  AiToolDefinition cancelAppointmentTool(CancelAuthorizedAppointmentUseCase u, ObjectMapper m) {
    return new AiToolDefinition(
        "cancelAppointment",
        "Cancel an appointment",
        Set.of("client", "tenant_staff", "tenant_owner", "admin"),
        AiToolRisk.MUTATION,
        true,
        false,
        new CancelAppointmentToolHandler(u, m),
        Set.of("appointmentId"),
        Set.of("appointmentId"));
  }

  @Bean
  AiToolDefinition rescheduleAppointmentTool(
      RescheduleAuthorizedAppointmentUseCase u, ObjectMapper m) {
    return new AiToolDefinition(
        "rescheduleAppointment",
        "Reschedule an appointment",
        Set.of("client", "tenant_staff", "tenant_owner", "admin"),
        AiToolRisk.MUTATION,
        true,
        false,
        new RescheduleAppointmentToolHandler(u, m),
        Set.of("appointmentId", "startsAt", "endsAt"),
        Set.of("appointmentId", "startsAt", "endsAt"));
  }
}
