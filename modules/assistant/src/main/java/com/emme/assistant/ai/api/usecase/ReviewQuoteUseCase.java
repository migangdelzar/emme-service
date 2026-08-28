package com.emme.assistant.ai.api.usecase;

import com.emme.assistant.ai.api.command.ReviewQuoteCommand;
import com.emme.assistant.ai.api.result.ReviewQuoteResult;

/** Resolves a pending staff quote review. */
public interface ReviewQuoteUseCase {

  ReviewQuoteResult review(ReviewQuoteCommand command);
}
