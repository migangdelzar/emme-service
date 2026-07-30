package com.emme.notification.entity;

import com.emme.kernel.type.NotificationChannel;
import com.emme.shared.TenantOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "notification")
public class Notification extends TenantOwnedEntity {

  @Enumerated(EnumType.STRING)
  @Column(name = "channel", nullable = false, length = 20)
  private NotificationChannel channel;

  @Column(name = "recipient_reference", nullable = false, length = 150)
  private String recipientReference;

  @Column(name = "body", length = 2000)
  private String body;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private NotificationStatus status = NotificationStatus.REQUESTED;

  protected Notification() {}

  public Notification(UUID tenantId, NotificationChannel channel, String recipientReference) {
    super(tenantId);
    this.channel = Objects.requireNonNull(channel, "channel must not be null");
    this.recipientReference =
        Objects.requireNonNull(recipientReference, "recipientReference must not be null");
  }

  public Notification(
      UUID tenantId, NotificationChannel channel, String recipientReference, String body) {
    this(tenantId, channel, recipientReference);
    this.body = body;
  }

  public NotificationChannel getChannel() {
    return channel;
  }

  public String getRecipientReference() {
    return recipientReference;
  }

  public String getBody() {
    return body;
  }

  public NotificationStatus getStatus() {
    return status;
  }

  public void setStatus(NotificationStatus status) {
    this.status = status;
  }

  public Instant getCreatedAt() {
    return super.getCreatedAt();
  }

  /** Transition from REQUESTED to SENT (stub delivery) */
  public void markSent() {
    if (status != NotificationStatus.REQUESTED) {
      throw new IllegalStateException("Cannot send notification with status: " + status);
    }
    status = NotificationStatus.SENT;
  }

  /** Transition from SENT to DELIVERED (stub delivery confirmation) */
  public void markDelivered() {
    if (status != NotificationStatus.SENT) {
      throw new IllegalStateException("Cannot deliver notification with status: " + status);
    }
    status = NotificationStatus.DELIVERED;
  }

  /** Transition from REQUESTED to CANCELLED */
  public void markCancelled() {
    if (status != NotificationStatus.REQUESTED) {
      throw new IllegalStateException("Cannot cancel notification with status: " + status);
    }
    status = NotificationStatus.CANCELLED;
  }

  /** Transition from any non-terminal status to FAILED */
  public void markFailed() {
    if (status == NotificationStatus.DELIVERED || status == NotificationStatus.CANCELLED) {
      throw new IllegalStateException("Cannot fail notification with terminal status: " + status);
    }
    status = NotificationStatus.FAILED;
  }
}
