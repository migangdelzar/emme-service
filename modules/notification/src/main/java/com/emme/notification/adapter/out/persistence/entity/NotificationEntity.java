package com.emme.notification.adapter.out.persistence.entity;

import com.emme.kernel.type.NotificationChannel;
import com.emme.notification.domain.model.NotificationStatus;
import com.emme.shared.persistence.TenantOwnedEntity;
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
public class NotificationEntity extends TenantOwnedEntity {

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

  protected NotificationEntity() {}

  public NotificationEntity(UUID tenantId, NotificationChannel channel, String recipientReference) {
    super(tenantId);
    this.channel = Objects.requireNonNull(channel, "channel must not be null");
    this.recipientReference =
        Objects.requireNonNull(recipientReference, "recipientReference must not be null");
  }

  public NotificationEntity(
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

  public void restoreIdentity(UUID id, Instant createdAt) {
    restoreAuditFields(id, createdAt, createdAt);
  }
}
