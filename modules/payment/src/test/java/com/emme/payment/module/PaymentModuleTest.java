package com.emme.payment.module;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emme.testing.BaseSpringModuleTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class PaymentModuleTest extends BaseSpringModuleTest {

  @BeforeEach
  void setUp() {
    fullSetup();
  }

  @Test
  void shouldCreatePayment() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/payments")
                .with(tenantJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"providerReference\":\"stripe-txn-001\",\"amount\":99.99,\"currency\":\"USD\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.amount").value(99.99))
        .andExpect(jsonPath("$.currency").value("USD"));
  }

  @Test
  void shouldGetPaymentById() throws Exception {
    var result =
        mockMvc
            .perform(
                post("/api/v1/payments")
                    .with(tenantJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"providerReference\":\"stripe-txn-002\",\"amount\":50.00,\"currency\":\"MXN\"}"))
            .andExpect(status().isCreated())
            .andReturn();

    String paymentId =
        result
            .getResponse()
            .getContentAsString()
            .replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");

    mockMvc
        .perform(get("/api/v1/payments/{id}", paymentId).with(tenantJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(paymentId))
        .andExpect(jsonPath("$.amount").value(50.00));
  }

  @Test
  void shouldRejectWithoutJwt() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"providerReference\":\"unauth\",\"amount\":10.00,\"currency\":\"USD\"}"))
        .andExpect(status().isUnauthorized());
  }
}
