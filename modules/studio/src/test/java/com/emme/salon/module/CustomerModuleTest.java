package com.emme.studio.module;

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

class CustomerModuleTest extends BaseSpringModuleTest {

  @BeforeEach
  void setUp() {
    fullSetup();
  }

  @Test
  void shouldCreateCustomer() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/customers")
                .with(tenantJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"Jane Doe\",\"phone\":\"555-1234\",\"email\":\"jane@example.com\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.name").value("Jane Doe"))
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }

  @Test
  void shouldListCustomers() throws Exception {
    // Create one first so list is non-empty
    mockMvc
        .perform(
            post("/api/v1/customers")
                .with(tenantJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"List Test\"}"))
        .andExpect(status().isCreated());

    mockMvc
        .perform(get("/api/v1/customers").with(tenantJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").isNotEmpty());
  }

  @Test
  void shouldUpdateCustomer() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/customers")
                    .with(tenantJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Original Name\"}"))
            .andExpect(status().isCreated())
            .andReturn();

    UUID customerId =
        UUID.fromString(
            result.getResponse().getContentAsString().replaceAll(".*\"id\":\"([^\"]+)\".*", "$1"));

    mockMvc
        .perform(
            put("/api/v1/customers/{id}", customerId)
                .with(tenantJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Updated Name\",\"phone\":\"555-9999\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Updated Name"))
        .andExpect(jsonPath("$.phone").value("555-9999"));
  }

  @Test
  void shouldGetCustomerById() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/customers")
                    .with(tenantJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Get By ID\"}"))
            .andExpect(status().isCreated())
            .andReturn();

    UUID customerId =
        UUID.fromString(
            result.getResponse().getContentAsString().replaceAll(".*\"id\":\"([^\"]+)\".*", "$1"));

    mockMvc
        .perform(get("/api/v1/customers/{id}", customerId).with(tenantJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(customerId.toString()));
  }
}
