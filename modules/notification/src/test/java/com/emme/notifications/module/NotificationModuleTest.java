package com.emme.notification.module;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emme.testing.BaseSpringModuleTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class NotificationModuleTest extends BaseSpringModuleTest {

  @BeforeEach
  void setUp() {
    fullSetup();
  }

  @Test
  void shouldListNotifications() throws Exception {
    mockMvc.perform(get("/api/v1/notifications").with(tenantJwt())).andExpect(status().isOk());
  }

  @Test
  void shouldSendNotification() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/notifications")
                .with(tenantJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"channel\":\"EMAIL\",\"recipient\":\"client@example.com\",\"message\":\"Appointment reminder\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.channel").value("EMAIL"))
        .andExpect(jsonPath("$.status").value("REQUESTED"));
  }

  @Test
  void shouldRejectWithoutJwt() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"channel\":\"EMAIL\",\"recipient\":\"test@example.com\",\"message\":\"Test\"}"))
        .andExpect(status().isUnauthorized());
  }
}
