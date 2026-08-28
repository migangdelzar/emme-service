package com.emme.assistant.ai.domain.quote;

import java.math.BigDecimal;

/** Applied deterministic quote line. */
public record QuoteLine(
    String code,
    QuoteLineType type,
    BigDecimal minimumPrice,
    BigDecimal maximumPrice,
    int durationMinutes) {}
