package com.emme.e2e.tests;

import static com.emme.client.E2eTest.withSession;
import static com.emme.client.E2eTest.withUnauthenticated;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiApiTest {

  @Test
  void shouldUseTheConfiguredMockProvider() {
    withSession(
        s -> {
          String body =
              """
                {"userMessage":"Hello","conversationContext":"e2e-test"}
                """;
          var result = s.post("/api/ai/chat", body, 200);
          assertThat(result).contains("MOCK");
        });
  }

  @Test
  void shouldRejectAiChatWithoutAuth() {
    withUnauthenticated(
        s -> {
          String body =
              """
                {"userMessage":"Hello"}
                """;
          var result = s.post("/api/ai/chat", body, 401);
          assertThat(result).isNotNull();
        });
  }
}
