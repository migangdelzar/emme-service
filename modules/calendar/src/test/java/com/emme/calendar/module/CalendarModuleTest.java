package com.emme.calendar.module;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emme.tenancy.testing.EntitledTenantModuleTest;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CalendarModuleTest extends EntitledTenantModuleTest {

  @BeforeEach
  void setUp() {
    fullSetup();
  }

  @Test
  void shouldGetBusyTimes() throws Exception {
    mockMvc
        .perform(
            get("/api/calendar/busy")
                .with(tenantJwt())
                .param("artistId", UUID.randomUUID().toString())
                .param("date", LocalDate.now().plusDays(1).toString()))
        .andExpect(status().isOk());
  }

  @Test
  void shouldTriggerSync() throws Exception {
    mockMvc
        .perform(post("/api/calendar/sync").with(tenantJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").exists());
  }

  @Test
  void shouldRejectWithoutJwt() throws Exception {
    mockMvc
        .perform(
            get("/api/calendar/busy")
                .param("artistId", UUID.randomUUID().toString())
                .param("date", LocalDate.now().toString()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void shouldReturnBadRequestWithoutParams() throws Exception {
    mockMvc.perform(get("/api/calendar/busy").with(tenantJwt())).andExpect(status().isBadRequest());
  }
}
