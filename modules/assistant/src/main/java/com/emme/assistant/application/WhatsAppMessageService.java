package com.emme.assistant.application;

import com.emme.assistant.ai.application.AiService;
import com.emme.assistant.entity.ChannelParticipant;
import com.emme.assistant.entity.ChannelParticipantRepository;
import com.emme.assistant.entity.Conversation;
import com.emme.assistant.entity.ConversationStatus;
import com.emme.kernel.type.ChannelType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnExpression("not '${app.whatsapp.verify-token:}'.isEmpty()")
@Transactional
public class WhatsAppMessageService {

  private static final Logger log = LoggerFactory.getLogger(WhatsAppMessageService.class);

  private final String appSecret;
  private final UUID defaultTenantId;
  private final ConversationService conversationService;
  private final AiService aiService;
  private final ChannelParticipantRepository participantRepo;
  private final ObjectMapper objectMapper;
  private final OkHttpClient httpClient;

  public WhatsAppMessageService() {
    // Default constructor for Spring proxying — not used in production
    this.appSecret = "";
    this.defaultTenantId = UUID.randomUUID();
    this.conversationService = null;
    this.aiService = null;
    this.participantRepo = null;
    this.objectMapper = new ObjectMapper();
    this.httpClient = new OkHttpClient();
  }

  public WhatsAppMessageService(
      @Value("${app.whatsapp.app-secret:}") String appSecret,
      @Value("${app.whatsapp.tenant-id:00000000-0000-0000-0000-000000000000}") String tenantIdStr,
      ConversationService conversationService,
      AiService aiService,
      ChannelParticipantRepository participantRepo) {
    this.appSecret = appSecret;
    this.defaultTenantId = uuidOrFallback(tenantIdStr);
    this.conversationService = conversationService;
    this.aiService = aiService;
    this.participantRepo = participantRepo;
    this.objectMapper = new ObjectMapper();
    this.httpClient = new OkHttpClient();
  }

  /** Test constructor — allows injecting a custom OkHttpClient. */
  public WhatsAppMessageService(
      String appSecret,
      String tenantIdStr,
      ConversationService conversationService,
      AiService aiService,
      ChannelParticipantRepository participantRepo,
      OkHttpClient httpClient) {
    this.appSecret = appSecret;
    this.defaultTenantId = uuidOrFallback(tenantIdStr);
    this.conversationService = conversationService;
    this.aiService = aiService;
    this.participantRepo = participantRepo;
    this.objectMapper = new ObjectMapper();
    this.httpClient = httpClient;
  }

  private static UUID uuidOrFallback(String s) {
    if (s == null || s.isBlank() || "00000000-0000-0000-0000-000000000000".equals(s)) {
      return UUID.fromString("00000000-0000-0000-0000-000000000000");
    }
    try {
      return UUID.fromString(s);
    } catch (IllegalArgumentException e) {
      log.warn("Invalid tenant-id '{}', using fallback", s);
      return UUID.fromString("00000000-0000-0000-0000-000000000000");
    }
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

    log.info("Processing WhatsApp message from={} text={}", msg.from(), msg.text());

    // 3. Find or create ChannelParticipant
    ChannelParticipant participant = findOrCreateParticipant(msg.tenantId(), msg.from());

    // 4. Find active conversation or create new one
    Conversation conv = findOrCreateConversation(msg.tenantId(), participant);

    // 5. Store inbound message as ConversationEvent
    conversationService.addEvent(conv.getId(), "MESSAGE_RECEIVED", msg.text());

    // 6. Trigger AI response
    String aiResponse = aiService.chat("", msg.text());
    conversationService.addEvent(conv.getId(), "MESSAGE_SENT", aiResponse);

    log.info("AI response for conversation={}: {}", conv.getId(), aiResponse);

    // 7. Send reply via WhatsApp Cloud API
    sendReply(msg.from(), aiResponse);
  }

  /**
   * Verify the HMAC-SHA256 signature from the X-Hub-Signature-256 header. Meta sends "sha256=<hex>"
   * — we recompute and compare.
   */
  public boolean verifySignature(String payload, String signature) {
    if (appSecret == null || appSecret.isBlank()) {
      log.warn("WhatsApp app-secret not configured — skipping signature verification");
      return true;
    }
    if (signature == null || !signature.startsWith("sha256=")) {
      log.warn("Invalid signature format: {}", signature);
      return false;
    }

    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      SecretKeySpec keySpec =
          new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
      mac.init(keySpec);
      byte[] computed = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
      String expected = "sha256=" + HexFormat.of().formatHex(computed);

      boolean valid = expected.equals(signature);
      if (!valid) {
        log.warn(
            "Signature mismatch: expected={} received={}",
            expected.substring(0, 20) + "...",
            signature.substring(0, 20) + "...");
      }
      return valid;
    } catch (Exception e) {
      log.error("HMAC verification error", e);
      return false;
    }
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
            return new WhatsAppMessage(defaultTenantId, "", "", true);
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

            return new WhatsAppMessage(tenantId, from, text, false);
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
    // TODO: map phone_number_id / WABA ID → tenant via config or DB lookup
    return defaultTenantId;
  }

  public ChannelParticipant findOrCreateParticipant(UUID tenantId, String fromNumber) {
    return participantRepo
        .findByTenantIdAndChannelAndProviderReference(tenantId, ChannelType.WHATSAPP, fromNumber)
        .orElseGet(
            () -> {
              log.info("Creating new ChannelParticipant for {} via WhatsApp", fromNumber);
              ChannelParticipant p =
                  new ChannelParticipant(tenantId, ChannelType.WHATSAPP, fromNumber);
              return participantRepo.save(p);
            });
  }

  public Conversation findOrCreateConversation(UUID tenantId, ChannelParticipant participant) {
    List<Conversation> conversations = conversationService.findByTenantId(tenantId);
    return conversations.stream()
        .filter(c -> c.getParticipantId().equals(participant.getId()))
        .filter(c -> c.getStatus() == ConversationStatus.ACTIVE)
        .findFirst()
        .orElseGet(
            () -> {
              log.info("Creating new conversation for participant={}", participant.getId());
              return conversationService.startConversation(
                  tenantId, participant.getId(), ChannelType.WHATSAPP);
            });
  }

  /**
   * Send a text reply to a WhatsApp user via the Meta Graph API (v21.0). Requires
   * WHATSAPP_ACCESS_TOKEN and WHATSAPP_PHONE_ID environment variables.
   */
  public void sendReply(String to, String text) {
    String accessToken = System.getenv("WHATSAPP_ACCESS_TOKEN");
    String phoneNumberId = System.getenv("WHATSAPP_PHONE_ID");
    sendReply(to, text, accessToken, phoneNumberId, "https://graph.facebook.com/v21.0");
  }

  /** Package-private overload for testing with explicit credentials and API base URL. */
  public void sendReply(
      String to, String text, String accessToken, String phoneNumberId, String apiBaseUrl) {
    if (accessToken == null || phoneNumberId == null) {
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

  public record WhatsAppMessage(UUID tenantId, String from, String text, boolean isStatusUpdate) {}
}
