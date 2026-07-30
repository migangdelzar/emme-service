package com.emme.payment.web;

import com.emme.kernel.context.TenantContextHolder;
import com.emme.payment.application.PaymentService;
import com.emme.payment.provider.PaymentProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles MercadoPago IPN (Instant Payment Notification) webhook callbacks.
 *
 * <p>MercadoPago sends POST notifications to the configured notification_url when payment status
 * changes. The request body is JSON with 'topic' and 'id' fields. The x-signature header contains
 * HMAC-SHA256 of the body, hashed with the webhook secret.
 */
@RestController
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "mercadopago")
class MercadoPagoWebhookController {

  private static final Logger log = LoggerFactory.getLogger(MercadoPagoWebhookController.class);

  private final PaymentService paymentService;
  private final String webhookSecret;

  MercadoPagoWebhookController(PaymentService paymentService, PaymentProperties props) {
    this.paymentService = paymentService;
    this.webhookSecret = System.getenv("MP_WEBHOOK_SECRET");
  }

  @PostMapping("/api/v1/callbacks/payments")
  ResponseEntity<String> handleCallback(HttpServletRequest request) {
    String rawBody;
    try {
      rawBody = request.getReader().lines().reduce("", (acc, line) -> acc + line);
    } catch (Exception e) {
      log.error("Failed to read webhook request body", e);
      return ResponseEntity.badRequest().body("Invalid request body");
    }

    String signature = request.getHeader("x-signature");

    // Verify signature if webhook secret is configured
    if (webhookSecret != null && !webhookSecret.isBlank()) {
      if (signature == null || signature.isBlank()) {
        log.warn("MercadoPago webhook received without x-signature header");
        return ResponseEntity.status(401).body("Missing signature");
      }
      if (!verifySignature(rawBody, signature, webhookSecret)) {
        log.warn("MercadoPago webhook signature verification failed");
        return ResponseEntity.status(401).body("Invalid signature");
      }
    } else {
      log.warn("MP_WEBHOOK_SECRET not configured — skipping signature verification");
    }

    UUID tenantId =
        TenantContextHolder.currentTenantOptional()
            .orElse(UUID.fromString("00000000-0000-0000-0000-000000000000"));

    Map<String, String> payload = extractPayload(rawBody);
    paymentService.processCallback(tenantId, payload, signature != null ? signature : "");

    return ResponseEntity.ok("OK");
  }

  @SuppressWarnings("unchecked")
  private Map<String, String> extractPayload(String rawBody) {
    try {
      com.fasterxml.jackson.databind.ObjectMapper mapper =
          new com.fasterxml.jackson.databind.ObjectMapper();
      Map<String, Object> raw = mapper.readValue(rawBody, Map.class);
      return Map.of(
          "topic", String.valueOf(raw.getOrDefault("topic", "payment")),
          "id", String.valueOf(raw.getOrDefault("id", "")));
    } catch (Exception e) {
      log.error("Failed to parse webhook payload: {}", rawBody, e);
      return Map.of("topic", "payment", "id", "parse_error");
    }
  }

  private boolean verifySignature(String body, String signature, String secret) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      SecretKeySpec keySpec =
          new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
      mac.init(keySpec);
      byte[] hash = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
      String expected = Base64.getEncoder().encodeToString(hash);
      return expected.equals(signature);
    } catch (Exception e) {
      log.error("Signature verification error", e);
      return false;
    }
  }
}
