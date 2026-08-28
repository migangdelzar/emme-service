package com.emme.assistant.ai.adapter.out.provider.springai.advisor;

import java.util.Objects;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;

/** Adds the prompt contract version to model-call metadata for traceability. */
public final class PromptVersionAdvisor implements BaseAdvisor {

  private final String promptVersion;

  public PromptVersionAdvisor(String promptVersion) {
    if (promptVersion == null || promptVersion.isBlank()) {
      throw new IllegalArgumentException("promptVersion must not be blank");
    }
    this.promptVersion = promptVersion;
  }

  @Override
  public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
    Objects.requireNonNull(request, "request must not be null");
    return request.mutate().context("promptVersion", promptVersion).build();
  }

  @Override
  public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
    return response;
  }

  @Override
  public String getName() {
    return "emme-prompt-version";
  }

  @Override
  public int getOrder() {
    return -100;
  }
}
