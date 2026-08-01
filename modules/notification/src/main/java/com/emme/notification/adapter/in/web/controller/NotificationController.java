package com.emme.notification.adapter.in.web.controller;

import static com.emme.kernel.context.TenantContextHolder.withCurrentTenant;

import com.emme.kernel.type.NotificationChannel;
import com.emme.notification.adapter.out.persistence.entity.NotificationEntity;
import com.emme.notification.application.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications")
public class NotificationController {

  private final NotificationService notificationService;

  public NotificationController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @PostMapping
  @Operation(summary = "Request a notification")
  public ResponseEntity<NotificationResponse> request(
      @Valid @RequestBody RequestNotificationRequest request) {
    return withCurrentTenant(
        tenantId -> {
          NotificationEntity notification =
              notificationService.request(
                  tenantId, request.channel(), request.recipient(), request.message());
          var location = URI.create("/api/v1/notifications/" + notification.getId());
          return ResponseEntity.created(location).body(NotificationResponse.from(notification));
        });
  }

  @GetMapping
  @Operation(summary = "List notifications for current tenant")
  public ResponseEntity<List<NotificationResponse>> list() {
    return withCurrentTenant(
        tenantId ->
            ResponseEntity.ok(
                notificationService.findByTenantId(tenantId).stream()
                    .map(NotificationResponse::from)
                    .toList()));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a notification by ID")
  public ResponseEntity<NotificationResponse> get(@PathVariable UUID id) {
    NotificationEntity notification = notificationService.findById(id);
    return ResponseEntity.ok(NotificationResponse.from(notification));
  }

  // --- DTOs ---

  public record RequestNotificationRequest(
      @NotNull NotificationChannel channel, @NotBlank String recipient, @NotBlank String message) {}

  public record NotificationResponse(
      UUID id,
      UUID tenantId,
      String channel,
      String recipientReference,
      String status,
      Instant createdAt) {
    public static NotificationResponse from(NotificationEntity n) {
      return new NotificationResponse(
          n.getId(),
          n.getTenantId(),
          n.getChannel().name(),
          n.getRecipientReference(),
          n.getStatus().name(),
          n.getCreatedAt());
    }
  }
}
