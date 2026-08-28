package com.emme.assistant.ai.api.usecase;

import com.emme.assistant.ai.api.command.ProcessDesignQuoteCommand;
import com.emme.assistant.ai.api.result.QuoteWorkflowResult;

/** Starts or resumes the tenant-scoped design-quote workflow. */
public interface ProcessDesignQuoteUseCase {

  QuoteWorkflowResult process(ProcessDesignQuoteCommand command);
}
