package com.emme.assistant.ai.configuration;

import com.emme.appointments.api.usecase.BookAppointmentUseCase;
import com.emme.appointments.api.usecase.CancelAuthorizedAppointmentUseCase;
import com.emme.appointments.api.usecase.RescheduleAuthorizedAppointmentUseCase;
import com.emme.assistant.ai.adapter.out.tool.CancelAppointmentToolHandler;
import com.emme.assistant.ai.adapter.out.tool.CreateAppointmentToolHandler;
import com.emme.assistant.ai.adapter.out.tool.RescheduleAppointmentToolHandler;
import com.emme.assistant.ai.application.tool.AiToolDefinition;
import com.emme.assistant.ai.application.tool.AiToolRisk;
import com.emme.kernel.context.Channel;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registers appointment mutation tools against the appointments API. */
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
        Set.of("customerId", "serviceId", "artistId", "startsAt", "endsAt"),
        Set.of("appointments"),
        Set.of("ai_appointments"),
        Set.of(Channel.WEB, Channel.WHATSAPP));
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
        Set.of("appointmentId"),
        Set.of("appointments"),
        Set.of("ai_appointments"),
        Set.of(Channel.WEB, Channel.WHATSAPP));
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
        Set.of("appointmentId", "startsAt", "endsAt"),
        Set.of("appointments"),
        Set.of("ai_appointments"),
        Set.of(Channel.WEB, Channel.WHATSAPP));
  }
}
