package com.emme.assistant.ai.adapter.in.web.response;

import com.emme.assistant.ai.domain.workflow.QuoteWorkflowState;
import java.util.UUID;

public record DesignQuoteResponse(UUID workflowId, QuoteWorkflowState state) {}
