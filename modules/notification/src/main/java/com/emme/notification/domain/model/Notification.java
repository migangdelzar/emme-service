package com.emme.notification.domain.model;

import com.emme.kernel.type.NotificationChannel;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Framework-free notification aggregate owning delivery lifecycle invariants. */
public final class Notification {
  private final UUID id;
  private final UUID tenantId;
  private final NotificationChannel channel;
  private final String recipientReference;
  private final String body;
  private final Instant createdAt;
  private NotificationStatus status;

  public Notification(
      UUID tenantId, NotificationChannel channel, String recipientReference, String body) {
    this(
        null,
        tenantId,
        channel,
        recipientReference,
        body,
        NotificationStatus.REQUESTED,
        Instant.now());
  }

  private Notification(
      UUID id,
      UUID tenantId,
      NotificationChannel channel,
      String recipientReference,
      String body,
      NotificationStatus status,
      Instant createdAt) {
    this.id = id;
    this.tenantId = Objects.requireNonNull(tenantId);
    this.channel = Objects.requireNonNull(channel);
    this.recipientReference = Objects.requireNonNull(recipientReference);
    this.body = body;
    this.status = Objects.requireNonNull(status);
    this.createdAt = Objects.requireNonNull(createdAt);
  }

  public static Notification rehydrate(
      UUID id,
      UUID tenantId,
      NotificationChannel channel,
      String recipientReference,
      String body,
      NotificationStatus status,
      Instant createdAt) {
    return new Notification(
        Objects.requireNonNull(id, "id must not be null"),
        tenantId,
        channel,
        recipientReference,
        body,
        status,
        createdAt);
  }

  public UUID id() {
    return id;
  }

  public UUID tenantId() {
    return tenantId;
  }

  public NotificationChannel channel() {
    return channel;
  }

  public String recipientReference() {
    return recipientReference;
  }

  public String body() {
    return body;
  }

  public NotificationStatus status() {
    return status;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public void markSent() {
    if (status != NotificationStatus.REQUESTED)
      throw new IllegalStateException("Cannot send notification with status: " + status);
    status = NotificationStatus.SENT;
  }

  public void markDelivered() {
    if (status != NotificationStatus.SENT)
      throw new IllegalStateException("Cannot deliver notification with status: " + status);
    status = NotificationStatus.DELIVERED;
  }

  public void markCancelled() {
    if (status != NotificationStatus.REQUESTED)
      throw new IllegalStateException("Cannot cancel notification with status: " + status);
    status = NotificationStatus.CANCELLED;
  }

  public void markFailed() {
    if (status == NotificationStatus.DELIVERED || status == NotificationStatus.CANCELLED) {
      throw new IllegalStateException("Cannot fail notification with terminal status: " + status);
    }
    status = NotificationStatus.FAILED;
  }
}
