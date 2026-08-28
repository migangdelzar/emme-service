package com.emme.assistant.ai.adapter.in.web.request;

import com.emme.assistant.ai.domain.workflow.QuoteReviewDecisionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** HTTP command for resolving a pending quote review with optimistic locking. */
public record ReviewQuoteRequest(
    @NotNull @Min(0) Long expectedVersion,
    @NotNull QuoteReviewDecisionType decision,
    @Size(max = 4000) String notes) {}
