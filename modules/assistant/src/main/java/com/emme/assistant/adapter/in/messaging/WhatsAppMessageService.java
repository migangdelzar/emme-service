package com.emme.assistant.adapter.in.messaging;

import com.emme.assistant.adapter.in.webhook.WhatsAppWebhookSignatureVerifier;
import com.emme.assistant.ai.api.usecase.ChatUseCase;
import com.emme.assistant.api.command.AddConversationEventCommand;
import com.emme.assistant.api.command.StartConversationCommand;
import com.emme.assistant.api.query.ListConversationsQuery;
import com.emme.assistant.api.result.ConversationInfo;
import com.emme.assistant.api.usecase.AddConversationEventUseCase;
import com.emme.assistant.api.usecase.ListConversationsUseCase;
import com.emme.assistant.api.usecase.StartConversationUseCase;
import com.emme.assistant.application.port.out.ChannelParticipantRepository;
import com.emme.assistant.application.port.out.WhatsAppTenantResolver;
import com.emme.assistant.application.port.out.WhatsAppWebhookEventRepository;
import com.emme.assistant.configuration.WhatsAppProperties;
import com.emme.assistant.domain.model.ChannelParticipant;
import com.emme.assistant.domain.model.ConversationStatus;
import com.emme.kernel.type.ChannelType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnExpression("not '${app.whatsapp.verify-token:}'.isEmpty()")
@Transactional
public class WhatsAppMessageService {

  private static final Logger log = LoggerFactory.getLogger(WhatsAppMessageService.class);

  private final WhatsAppProperties properties;
  private final StartConversationUseCase startConversation;
  private final ListConversationsUseCase listConversations;
  private final AddConversationEventUseCase addConversationEvent;
  private final ChatUseCase chatUseCase;
  private final ChannelParticipantRepository participantRepository;
  private final WhatsAppWebhookSignatureVerifier signatureVerifier;
  private final WhatsAppTenantResolver tenantResolver;
  private final WhatsAppWebhookEventRepository webhookEvents;
  private final ObjectMapper objectMapper;
  private final OkHttpClient httpClient;

  /**
   * Constructor receives the signature verifier and HTTP client at the adapter composition root.
   */
  public WhatsAppMessageService(
      WhatsAppProperties properties,
      StartConversationUseCase startConversation,
      ListConversationsUseCase listConversations,
      AddConversationEventUseCase addConversationEvent,
      ChatUseCase chatUseCase,
      ChannelParticipantRepository participantRepository,
      WhatsAppWebhookSignatureVerifier signatureVerifier,
      WhatsAppTenantResolver tenantResolver,
      WhatsAppWebhookEventRepository webhookEvents,
      OkHttpClient httpClient) {
    this.properties = properties;
    this.startConversation = startConversation;
    this.listConversations = listConversations;
    this.addConversationEvent = addConversationEvent;
    this.chatUseCase = chatUseCase;
    this.participantRepository = participantRepository;
    this.signatureVerifier = signatureVerifier;
    this.tenantResolver = tenantResolver;
    this.webhookEvents = webhookEvents;
    this.objectMapper = new ObjectMapper();
    this.httpClient = httpClient;
  }

  /**
   * Process an incoming WhatsApp webhook message. Verifies HMAC-SHA256 signature, parses the
   * payload, finds or creates participant and conversation, stores the message, and triggers AI
   * response.
   */
  public void processMessage(String payload, String signature) {
    // 1. Verify HMAC-SHA256 signature
    if (!verifySignature(payload, signature)) {
      throw new SecurityException("Invalid WhatsApp signature");
    }

    // 2. Parse Meta webhook payload
    WhatsAppMessage msg = parseMessage(payload);
    if (msg == null || msg.isStatusUpdate()) {
      log.debug("Ignoring null or status-update message");
      return;
    }
    if (msg.eventId() == null || msg.eventId().isBlank()) {
      throw new SecurityException("WhatsApp webhook event id is required");
    }
    if (!webhookEvents.claim(msg.tenantId(), "whatsapp", msg.eventId())) {
      log.info("Ignoring duplicate WhatsApp webhook event");
      return;
    }

    log.info("Processing WhatsApp message from={}", msg.from());

    // 3. Find or create ChannelParticipant
    ChannelParticipant participant = findOrCreateParticipant(msg.tenantId(), msg.from());

    // 4. Find active conversation or create new one
    ConversationInfo conv = findOrCreateConversation(msg.tenantId(), participant);

    // 5. Store inbound message as ConversationEvent
    addConversationEvent.add(
        new AddConversationEventCommand(conv.id(), "MESSAGE_RECEIVED", msg.text()));

    // 6. Trigger AI response
    String aiResponse = chatUseCase.chat("", msg.text());
    addConversationEvent.add(
        new AddConversationEventCommand(conv.id(), "MESSAGE_SENT", aiResponse));

    log.info("AI response generated for conversation={}", conv.id());

    // 7. Send reply via WhatsApp Cloud API
    sendReply(msg.from(), aiResponse);
  }

  /**
   * Verify the HMAC-SHA256 signature from the X-Hub-Signature-256 header. Meta sends "sha256=<hex>"
   * — we recompute and compare.
   */
  public boolean verifySignature(String payload, String signature) {
    if (properties.appSecret().isBlank()) {
      log.error("WhatsApp app-secret not configured; refusing webhook processing");
      return false;
    }
    return signatureVerifier.verify(payload, signature, properties.appSecret());
  }

  /**
   * Parse the Meta webhook JSON payload into a WhatsAppMessage.
   *
   * <p>Expected structure: { "entry": [{ "changes": [{ "value": { "messages": [{ "from": "...",
   * "text": { "body": "..." } }] } }] }] }
   *
   * <p>Status updates (delivered/read/sent) contain "statuses" instead of "messages" and are
   * filtered out by returning isStatusUpdate=true.
   */
  public WhatsAppMessage parseMessage(String payload) {
    try {
      JsonNode root = objectMapper.readTree(payload);
      JsonNode entries = root.get("entry");
      if (entries == null || !entries.isArray() || entries.isEmpty()) {
        log.debug("No entries in webhook payload");
        return null;
      }

      for (JsonNode entry : entries) {
        JsonNode changes = entry.get("changes");
        if (changes == null || !changes.isArray()) continue;

        for (JsonNode change : changes) {
          JsonNode value = change.get("value");
          if (value == null) continue;

          // Status updates (delivered, sent, read) — ignore
          JsonNode statuses = value.get("statuses");
          if (statuses != null && statuses.isArray() && !statuses.isEmpty()) {
            log.debug("Ignoring status update");
            return new WhatsAppMessage(
                tenantResolver.resolve(value.path("metadata").path("phone_number_id").asText()),
                firstStatusId(statuses),
                "",
                "",
                true);
          }

          // Inbound text messages
          JsonNode messages = value.get("messages");
          if (messages != null && messages.isArray() && !messages.isEmpty()) {
            JsonNode msg = messages.get(0);
            String type = msg.has("type") ? msg.get("type").asText() : "text";

            String from = msg.get("from").asText();
            String text = extractMessageText(msg, type);

            // Resolve tenant from phone_number_id metadata
            UUID tenantId = resolveTenant(value);

            return new WhatsAppMessage(tenantId, msg.path("id").asText(""), from, text, false);
          }
        }
      }
      log.debug("No messages found in webhook payload");
      return null;
    } catch (Exception e) {
      log.error("Failed to parse WhatsApp webhook payload", e);
      return null;
    }
  }

  private String extractMessageText(JsonNode msg, String type) {
    return switch (type) {
      case "text" -> {
        JsonNode textNode = msg.get("text");
        yield textNode != null && textNode.has("body") ? textNode.get("body").asText("") : "";
      }
      case "button" -> {
        JsonNode btnNode = msg.get("button");
        yield btnNode != null && btnNode.has("text") ? btnNode.get("text").asText("") : "";
      }
      case "interactive" -> {
        JsonNode interactive = msg.get("interactive");
        if (interactive != null) {
          if (interactive.has("button_reply")) {
            yield interactive.get("button_reply").get("title").asText("");
          }
          if (interactive.has("list_reply")) {
            yield interactive.get("list_reply").get("title").asText("");
          }
        }
        yield "";
      }
      default -> "[non-text message: " + type + "]";
    };
  }

  /**
   * Resolve tenant UUID from metadata phone_number_id. Uses the configured default tenant for now.
   * Future: lookup by WABA ID.
   */
  private UUID resolveTenant(JsonNode value) {
    return tenantResolver.resolve(value.path("metadata").path("phone_number_id").asText());
  }

  private String firstStatusId(JsonNode statuses) {
    return statuses.path(0).path("id").asText("");
  }

  public ChannelParticipant findOrCreateParticipant(UUID tenantId, String fromNumber) {
    return participantRepository
        .findByTenantIdAndChannelAndProviderReference(tenantId, ChannelType.WHATSAPP, fromNumber)
        .orElseGet(
            () -> {
              log.info("Creating new ChannelParticipant for {} via WhatsApp", fromNumber);
              ChannelParticipant participant =
                  new ChannelParticipant(
                      null,
                      tenantId,
                      ChannelType.WHATSAPP,
                      fromNumber,
                      null,
                      com.emme.assistant.domain.model.ConsentStatus.UNKNOWN);
              return participantRepository.save(participant);
            });
  }

  public ConversationInfo findOrCreateConversation(UUID tenantId, ChannelParticipant participant) {
    List<ConversationInfo> conversations =
        listConversations.list(new ListConversationsQuery(tenantId));
    return conversations.stream()
        .filter(c -> c.participantId().equals(participant.id()))
        .filter(c -> c.status() == ConversationStatus.ACTIVE)
        .findFirst()
        .orElseGet(
            () -> {
              log.info("Creating new conversation for participant={}", participant.id());
              return startConversation.start(
                  new StartConversationCommand(tenantId, participant.id(), ChannelType.WHATSAPP));
            });
  }

  /**
   * Send a text reply to a WhatsApp user via the Meta Graph API (v21.0). Requires typed
   * WhatsAppProperties credentials.
   */
  public void sendReply(String to, String text) {
    sendReply(
        to, text, properties.accessToken(), properties.phoneNumberId(), properties.apiBaseUrl());
  }

  /** Package-private overload for testing with explicit credentials and API base URL. */
  public void sendReply(
      String to, String text, String accessToken, String phoneNumberId, String apiBaseUrl) {
    if (accessToken == null
        || accessToken.isBlank()
        || phoneNumberId == null
        || phoneNumberId.isBlank()
        || apiBaseUrl == null
        || apiBaseUrl.isBlank()) {
      log.warn("WhatsApp credentials not configured — cannot send reply");
      return;
    }

    try {
      String body =
          objectMapper.writeValueAsString(
              Map.of(
                  "messaging_product", "whatsapp",
                  "recipient_type", "individual",
                  "to", to,
                  "type", "text",
                  "text", Map.of("body", text)));

      Request req =
          new Request.Builder()
              .url(apiBaseUrl + "/" + phoneNumberId + "/messages")
              .header("Authorization", "Bearer " + accessToken)
              .header("Content-Type", "application/json")
              .post(RequestBody.create(body, MediaType.get("application/json")))
              .build();

      try (Response res = httpClient.newCall(req).execute()) {
        if (!res.isSuccessful()) {
          log.error(
              "WhatsApp send failed: {} - {}",
              res.code(),
              res.body() != null ? res.body().string() : "no body");
        } else {
          log.info("WhatsApp message sent to {}", to);
        }
      }
    } catch (IOException e) {
      log.error("WhatsApp send error: {}", e.getMessage());
    }
  }

  public record WhatsAppMessage(
      UUID tenantId, String eventId, String from, String text, boolean isStatusUpdate) {}
}
