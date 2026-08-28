package com.emme.assistant.ai.application.port.out;

import com.emme.assistant.ai.domain.quote.QuoteTemplate;
import java.util.Optional;

/** Tenant-scoped source of versioned quote templates. Implementations resolve tenant context. */
public interface QuoteTemplateRepository {

  Optional<QuoteTemplate> findByKey(String templateKey);
}
