package com.emme.services.module;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emme.testing.BaseSpringModuleTest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class ServiceModuleTest extends BaseSpringModuleTest {

  @BeforeEach
  void setUp() {
    fullSetup();
  }

  @Test
  void shouldCreateService() throws Exception {
    mockMvc
        .perform(
            post("/api/services")
                .with(tenantJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"code\":\"pedi\",\"name\":\"Pedicure\",\"durationMinutes\":60,\"basePrice\":45.00}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.code").value("pedi"))
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }

  @Test
  void shouldListServices() throws Exception {
    // Create one first
    mockMvc
        .perform(
            post("/api/services")
                .with(tenantJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"code\":\"gel\",\"name\":\"Gel Nails\",\"durationMinutes\":90,\"basePrice\":55.00}"))
        .andExpect(status().isCreated());

    mockMvc
        .perform(get("/api/services").with(tenantJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").isNotEmpty());
  }

  @Test
  void shouldUpdateServicePrice() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/services")
                    .with(tenantJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"code\":\"wax\",\"name\":\"Waxing\",\"durationMinutes\":30,\"basePrice\":20.00}"))
            .andExpect(status().isCreated())
            .andReturn();

    UUID serviceId =
        UUID.fromString(
            result.getResponse().getContentAsString().replaceAll(".*\"id\":\"([^\"]+)\".*", "$1"));

    mockMvc
        .perform(
            put("/api/services/{id}", serviceId)
                .with(tenantJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"Waxing Premium\",\"durationMinutes\":30,\"basePrice\":35.00}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.basePrice").value(35.00));
  }
}
