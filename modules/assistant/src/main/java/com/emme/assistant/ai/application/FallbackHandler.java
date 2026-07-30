package com.emme.assistant.ai.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FallbackHandler {

  private static final Logger log = LoggerFactory.getLogger(FallbackHandler.class);

  private static final String FALLBACK_MESSAGE =
      "I'm sorry, I'm having trouble processing your request. "
          + "Please try again or contact the salon directly.";

  public String handleFailure(Throwable error) {
    log.warn("AI service failure — returning fallback", error);
    return FALLBACK_MESSAGE;
  }
}
