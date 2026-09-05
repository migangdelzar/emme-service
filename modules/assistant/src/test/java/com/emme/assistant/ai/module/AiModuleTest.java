package com.emme.assistant.ai.module;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emme.tenancy.testing.BaseTenantModuleTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AiModuleTest extends BaseTenantModuleTest {

  @BeforeEach
  void setUp() {
    fullSetup();
  }

  @Test
  void shouldGetAiResponse() throws Exception {
    mockMvc
        .perform(
            post("/api/ai/chat")
                .with(tenantJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userMessage\":\"Hello, how are you?\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.response").exists());
  }

  @Test
  void shouldRejectWithoutJwt() throws Exception {
    mockMvc
        .perform(
            post("/api/ai/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userMessage\":\"Hello\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void shouldReturnMockProviderResponse() throws Exception {
    mockMvc
        .perform(
            post("/api/ai/chat")
                .with(tenantJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userMessage\":\"What services do you offer?\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.response").isString());
  }

  @Test
  void shouldDetectIntent() throws Exception {
    mockMvc
        .perform(
            post("/api/ai/intent")
                .with(tenantJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"I want to book an appointment\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.intent").exists());
  }

  @Test
  void shouldHandleEmptyMessageGracefully() throws Exception {
    mockMvc
        .perform(
            post("/api/ai/chat")
                .with(tenantJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userMessage\":\"\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.response").exists());
  }

  @Test
  void shouldHandleConversationContext() throws Exception {
    mockMvc
        .perform(
            post("/api/ai/chat")
                .with(tenantJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"userMessage\":\"Great, thanks!\",\"conversationContext\":\"Previous: user"
                        + " asked about nail services\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.response").exists());
  }
}
