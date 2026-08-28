package com.emme.assistant.ai.adapter.out.provider.springai.advisor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.prompt.Prompt;

class PromptVersionAdvisorTest {

  @Test
  void addsTheConfiguredPromptVersionToTheSpringAiContext() {
    PromptVersionAdvisor advisor = new PromptVersionAdvisor("chat-v4");

    ChatClientRequest request =
        advisor.before(new ChatClientRequest(new Prompt("hello"), Map.of()), null);

    assertThat(request.context()).containsEntry("promptVersion", "chat-v4");
  }
}
