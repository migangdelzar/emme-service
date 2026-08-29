package com.emme.assistant.ai.application.port.out;

/** Provider-neutral boundary for an answer generated with tenant-scoped retrieval augmentation. */
@FunctionalInterface
public interface RagAnswerPort {

  String answer(String question);
}
