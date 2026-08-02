package com.emme.studio.module;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emme.testing.BaseSpringModuleTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class BusinessConfigModuleTest extends BaseSpringModuleTest {

  @BeforeEach
  void setUp() {
    fullSetup();
  }

  @Test
  void shouldGetBusinessProfile() throws Exception {
    // Profile may not exist yet; create one then get
    mockMvc
        .perform(
            put("/api/business-config/profile")
                .with(tenantJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"displayName\":\"Test Salon\",\"timeZone\":\"America/Mexico_City\",\"locale\":\"es-MX\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/api/business-config/profile").with(tenantJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.displayName").value("Test Salon"));
  }

  @Test
  void shouldUpdateOperatingHours() throws Exception {
    mockMvc
        .perform(
            put("/api/business-config/hours")
                .with(tenantJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"day\":\"MON\",\"opensAt\":\"09:00:00\",\"closesAt\":\"18:00:00\",\"active\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.dayOfWeek").value("MON"))
        .andExpect(jsonPath("$.active").value(true));
  }
}
