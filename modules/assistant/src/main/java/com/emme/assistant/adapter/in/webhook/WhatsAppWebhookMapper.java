package com.emme.assistant.adapter.in.webhook;

import com.emme.assistant.application.port.out.WhatsAppTenantResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Maps Meta's webhook JSON into an application-neutral inbound message. */
@Component
public final class WhatsAppWebhookMapper {

  private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookMapper.class);

  private final ObjectMapper objectMapper;
  private final WhatsAppTenantResolver tenantResolver;

  public WhatsAppWebhookMapper(ObjectMapper objectMapper, WhatsAppTenantResolver tenantResolver) {
    this.objectMapper = objectMapper;
    this.tenantResolver = tenantResolver;
  }

  public Optional<WhatsAppWebhookMessage> map(String payload) {
    try {
      JsonNode root = objectMapper.readTree(payload);
      JsonNode entries = root.get("entry");
      if (entries == null || !entries.isArray() || entries.isEmpty()) {
        return Optional.empty();
      }

      for (JsonNode entry : entries) {
        JsonNode changes = entry.get("changes");
        if (changes == null || !changes.isArray()) continue;

        for (JsonNode change : changes) {
          JsonNode value = change.get("value");
          if (value == null) continue;

          JsonNode statuses = value.get("statuses");
          if (statuses != null && statuses.isArray() && !statuses.isEmpty()) {
            UUID tenantId = resolveTenant(value);
            String eventId = statuses.path(0).path("id").asText("");
            return Optional.of(new WhatsAppWebhookMessage(tenantId, eventId, "", "", true));
          }

          JsonNode messages = value.get("messages");
          if (messages != null && messages.isArray() && !messages.isEmpty()) {
            JsonNode message = messages.get(0);
            String type = message.path("type").asText("text");
            String from = message.path("from").asText("");
            String text = extractMessageText(message, type);
            return Optional.of(
                new WhatsAppWebhookMessage(
                    resolveTenant(value), message.path("id").asText(""), from, text, false));
          }
        }
      }
      return Optional.empty();
    } catch (Exception exception) {
      log.warn("Could not parse WhatsApp webhook payload", exception);
      return Optional.empty();
    }
  }

  private UUID resolveTenant(JsonNode value) {
    return tenantResolver.resolve(value.path("metadata").path("phone_number_id").asText());
  }

  private String extractMessageText(JsonNode message, String type) {
    return switch (type) {
      case "text" -> message.path("text").path("body").asText("");
      case "button" -> message.path("button").path("text").asText("");
      case "interactive" ->
          message.has("interactive")
              ? message.get("interactive").has("button_reply")
                  ? message.get("interactive").get("button_reply").path("title").asText("")
                  : message.get("interactive").has("list_reply")
                      ? message.get("interactive").get("list_reply").path("title").asText("")
                      : ""
              : "";
      default -> "[non-text message: " + type + "]";
    };
  }
}
