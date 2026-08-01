package com.emme.assistant.adapter.in.web.controller;

import com.emme.assistant.adapter.in.messaging.WhatsAppMessageService;
import com.emme.assistant.configuration.WhatsAppProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@ConditionalOnBean(WhatsAppMessageService.class)
@RestController
@RequestMapping("/api/v1/webhooks/whatsapp")
@Tag(name = "WhatsApp Webhook")
public class WhatsAppWebhookController {

  private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookController.class);

  private final WhatsAppMessageService messageService;
  private final WhatsAppProperties properties;

  public WhatsAppWebhookController(
      WhatsAppMessageService messageService, WhatsAppProperties properties) {
    this.messageService = messageService;
    this.properties = properties;
  }

  /** Meta webhook verification — GET with hub.mode, hub.verify_token, hub.challenge */
  @GetMapping
  @Operation(summary = "Verify WhatsApp webhook (Meta callback)")
  public ResponseEntity<String> verify(
      @RequestParam("hub.mode") String mode,
      @RequestParam("hub.verify_token") String token,
      @RequestParam("hub.challenge") String challenge) {

    if ("subscribe".equals(mode) && properties.verifyToken().equals(token)) {
      return ResponseEntity.ok(challenge);
    }
    return ResponseEntity.status(403).body("Verification failed");
  }

  /** Incoming WhatsApp messages — POST with signed payload */
  @PostMapping
  @Operation(summary = "Receive WhatsApp message (Meta callback)")
  @PreAuthorize("@featureFlagService.isEnabled('whatsapp_booking')")
  public ResponseEntity<String> receive(
      @RequestHeader("X-Hub-Signature-256") String signature, @RequestBody String payload) {

    try {
      messageService.processMessage(payload, signature);
      return ResponseEntity.ok("received");
    } catch (SecurityException e) {
      log.warn("WhatsApp webhook rejected: {}", e.getMessage());
      return ResponseEntity.status(403).body("Invalid signature");
    } catch (Exception e) {
      log.error("WhatsApp webhook processing error", e);
      return ResponseEntity.status(500).body("Processing error");
    }
  }
}
