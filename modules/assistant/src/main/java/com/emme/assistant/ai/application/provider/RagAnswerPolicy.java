package com.emme.assistant.ai.application.provider;

import com.emme.assistant.ai.application.port.out.ChatCompletionPort;
import com.emme.assistant.ai.application.port.out.RagAnswerPort;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Objects;

/** Applies RAG answer input policy while Spring AI owns retrieval augmentation. */
public final class RagAnswerPolicy implements RagAnswerPort {

  private final ChatCompletionPort completions;

  public RagAnswerPolicy(ChatCompletionPort completions) {
    this.completions = Objects.requireNonNull(completions, "completions must not be null");
  }

  @Override
  public String answer(String question) {
    AiExecutionContextScope.requireCurrent();
    if (question == null || question.isBlank()) {
      throw new IllegalArgumentException("question must not be blank");
    }
    return completions.complete("", question);
  }
}
