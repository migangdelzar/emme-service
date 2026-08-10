package com.emme.notification.adapter.in.web.controller;

import static com.emme.kernel.context.TenantContextHolder.withCurrentTenant;

import com.emme.notification.adapter.in.web.mapper.NotificationWebMapper;
import com.emme.notification.adapter.in.web.request.RequestNotificationRequest;
import com.emme.notification.adapter.in.web.response.NotificationResponse;
import com.emme.notification.api.query.GetNotificationQuery;
import com.emme.notification.api.query.ListNotificationsQuery;
import com.emme.notification.api.usecase.GetNotificationUseCase;
import com.emme.notification.api.usecase.ListNotificationsUseCase;
import com.emme.notification.api.usecase.RequestNotificationUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
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
@RequestMapping(path = "/api/notifications", version = "1.0")
@Tag(name = "Notifications")
public class NotificationController {
  private final RequestNotificationUseCase requestNotification;
  private final ListNotificationsUseCase listNotifications;
  private final GetNotificationUseCase getNotification;

  public NotificationController(
      RequestNotificationUseCase requestNotification,
      ListNotificationsUseCase listNotifications,
      GetNotificationUseCase getNotification) {
    this.requestNotification = requestNotification;
    this.listNotifications = listNotifications;
    this.getNotification = getNotification;
  }

  @PostMapping
  @Operation(summary = "Request a notification")
  public ResponseEntity<NotificationResponse> request(
      @Valid @RequestBody RequestNotificationRequest request) {
    return withCurrentTenant(
        tenantId -> {
          var notification =
              requestNotification.request(NotificationWebMapper.toCommand(tenantId, request));
          return ResponseEntity.created(URI.create("/api/notifications/" + notification.id()))
              .body(NotificationResponse.from(notification));
        });
  }

  @GetMapping
  @Operation(summary = "List notifications for current tenant")
  public ResponseEntity<List<NotificationResponse>> list() {
    return withCurrentTenant(
        tenantId ->
            ResponseEntity.ok(
                listNotifications.list(new ListNotificationsQuery(tenantId)).stream()
                    .map(NotificationResponse::from)
                    .toList()));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a notification by ID")
  public ResponseEntity<NotificationResponse> get(@PathVariable UUID id) {
    return withCurrentTenant(
        tenantId ->
            getNotification
                .get(new GetNotificationQuery(tenantId, id))
                .map(NotificationResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build()));
  }
}
