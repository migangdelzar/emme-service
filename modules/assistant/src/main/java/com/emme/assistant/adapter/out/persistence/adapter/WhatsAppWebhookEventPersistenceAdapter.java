package com.emme.assistant.adapter.out.persistence.adapter;

import com.emme.assistant.adapter.out.persistence.repository.SpringDataWhatsAppWebhookEventRepository;
import com.emme.assistant.application.port.out.WhatsAppWebhookEventRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Implements durable WhatsApp delivery claims with a database uniqueness constraint. */
@Component
@RequiredArgsConstructor
public class WhatsAppWebhookEventPersistenceAdapter implements WhatsAppWebhookEventRepository {
  private final SpringDataWhatsAppWebhookEventRepository repository;

  @Override
  public boolean claim(UUID tenantId, String provider, String eventId) {
    return repository.insertIfAbsent(tenantId, provider, eventId) == 1;
  }
}
