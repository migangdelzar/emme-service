package com.emme.assistant.adapter.in.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WhatsAppWebhookMapperTest {

  private final UUID tenantId = UUID.randomUUID();
  private final WhatsAppWebhookMapper mapper =
      new WhatsAppWebhookMapper(new ObjectMapper(), providerAccount -> tenantId);

  @Test
  void mapsInboundTextMessageAndResolvesTenantFromProviderAccount() {
    String payload =
        """
        {"entry":[{"changes":[{"value":{"metadata":{"phone_number_id":"phone-1"},"messages":[{"id":"event-1","from":"5215555555555","type":"text","text":{"body":"Hello"}}]}}]}]}
        """;

    assertThat(mapper.map(payload))
        .contains(new WhatsAppWebhookMessage(tenantId, "event-1", "5215555555555", "Hello", false));
  }

  @Test
  void mapsStatusUpdatesSoTheInboundAdapterCanIgnoreThem() {
    String payload =
        """
        {"entry":[{"changes":[{"value":{"metadata":{"phone_number_id":"phone-1"},"statuses":[{"id":"status-1"}]}}]}]}
        """;

    assertThat(mapper.map(payload).orElseThrow().statusUpdate()).isTrue();
  }

  @Test
  void ignoresMalformedOrEmptyPayloads() {
    assertThat(mapper.map("not-json")).isEmpty();
    assertThat(mapper.map("{\"entry\":[]}")).isEmpty();
  }
}
