package com.emme.assistant.module;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emme.testing.BaseSpringModuleTest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ConversationModuleTest extends BaseSpringModuleTest {

  @BeforeEach
  void setUp() {
    fullSetup();
  }

  @Test
  void shouldListConversations() throws Exception {
    mockMvc.perform(get("/api/v1/conversations").with(tenantJwt())).andExpect(status().isOk());
  }

  @Test
  void shouldGetConversationById() throws Exception {
    var result =
        mockMvc
            .perform(
                post("/api/v1/conversations")
                    .with(tenantJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"participantId\":\""
                            + UUID.randomUUID()
                            + "\",\"channel\":\"WHATSAPP\"}"))
            .andExpect(status().isCreated())
            .andReturn();

    String location = result.getResponse().getHeader("Location");
    String convId =
        location != null
            ? location.substring(location.lastIndexOf("/") + 1)
            : extractId(result.getResponse().getContentAsString());

    mockMvc
        .perform(get("/api/v1/conversations/{id}", convId).with(tenantJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(convId));
  }

  @Test
  void shouldCreateConversation() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/conversations")
                .with(tenantJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"participantId\":\"" + UUID.randomUUID() + "\",\"channel\":\"WEB_CHAT\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }

  @Test
  void shouldRejectWithoutJwt() throws Exception {
    mockMvc.perform(get("/api/v1/conversations")).andExpect(status().isUnauthorized());
  }

  private static String extractId(String json) {
    return json.replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");
  }
}
