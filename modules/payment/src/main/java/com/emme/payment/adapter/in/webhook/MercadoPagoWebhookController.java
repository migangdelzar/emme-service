package com.emme.payment.adapter.in.webhook;

import com.emme.kernel.context.TenantContextHolder;
import com.emme.payment.api.command.ProcessPaymentCallbackCommand;
import com.emme.payment.api.usecase.ProcessPaymentWorkflowCallbackUseCase;
import com.emme.payment.configuration.PaymentProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inbound MercadoPago webhook adapter.
 *
 * <p>The adapter authenticates the provider manifest, validates the payload, requires an explicit
 * tenant context, and delegates exactly once through the payment application contract.
 */
@RestController
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "mercadopago")
class MercadoPagoWebhookController {

  private static final Logger log = LoggerFactory.getLogger(MercadoPagoWebhookController.class);
  private static final String TENANT_HEADER = "X-Tenant-ID";

  private final ProcessPaymentWorkflowCallbackUseCase processPaymentCallback;
  private final String webhookSecret;
  private final ObjectMapper objectMapper;
  private final MercadoPagoWebhookSignatureVerifier signatureVerifier;

  MercadoPagoWebhookController(
      ProcessPaymentWorkflowCallbackUseCase processPaymentCallback,
      PaymentProperties props,
      ObjectMapper objectMapper,
      MercadoPagoWebhookSignatureVerifier signatureVerifier) {
    this.processPaymentCallback = processPaymentCallback;
    this.webhookSecret = props.mercadopago().webhookSecret();
    this.objectMapper = objectMapper;
    this.signatureVerifier = signatureVerifier;
  }

  @PostMapping("/api/callbacks/payments")
  ResponseEntity<String> handleCallback(HttpServletRequest request) {
    if (webhookSecret == null || webhookSecret.isBlank()) {
      log.error("MercadoPago webhook secret is not configured; refusing callback processing");
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body("Payment webhook is not configured");
    }

    String rawBody;
    try {
      rawBody = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException exception) {
      log.warn("Failed to read MercadoPago webhook request body", exception);
      return ResponseEntity.badRequest().body("Invalid request body");
    }

    JsonNode payload;
    try {
      payload = objectMapper.readTree(rawBody);
    } catch (JsonProcessingException exception) {
      log.warn("Rejected malformed MercadoPago webhook payload", exception);
      return ResponseEntity.badRequest().body("Invalid request body");
    }

    String dataId = dataId(payload).orElseGet(() -> request.getParameter("data.id"));
    String signature = request.getHeader("x-signature");
    String requestId = request.getHeader("x-request-id");
    if (!signatureVerifier.verify(signature, requestId, dataId, webhookSecret)) {
      log.warn("MercadoPago webhook signature verification failed");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
    }

    UUID tenantId;
    try {
      tenantId =
          tenantId(request).orElseThrow(() -> new IllegalArgumentException("tenant missing"));
    } catch (IllegalArgumentException exception) {
      return ResponseEntity.badRequest().body("Tenant context is required");
    }

    Map<String, String> callbackPayload = extractPayload(payload, dataId);
    processPaymentCallback.process(
        new ProcessPaymentCallbackCommand(
            tenantId, "mercadopago", requestId, callbackPayload, signature));
    return ResponseEntity.ok("OK");
  }

  private Map<String, String> extractPayload(JsonNode payload, String dataId) {
    String topic = payload.path("type").asText(payload.path("topic").asText("payment"));
    Map<String, String> result = new HashMap<>();
    result.put("topic", topic);
    result.put("id", dataId == null ? "" : dataId);
    return Map.copyOf(result);
  }

  private Optional<String> dataId(JsonNode payload) {
    String nested = payload.path("data").path("id").asText("");
    if (!nested.isBlank()) {
      return Optional.of(nested);
    }
    String direct = payload.path("id").asText("");
    return direct.isBlank() ? Optional.empty() : Optional.of(direct);
  }

  private Optional<UUID> tenantId(HttpServletRequest request) {
    Optional<UUID> contextTenant = TenantContextHolder.currentTenantOptional();
    if (contextTenant.isPresent()) {
      return contextTenant;
    }
    String header = request.getHeader(TENANT_HEADER);
    if (header == null || header.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(UUID.fromString(header));
  }
}
