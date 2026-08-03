package com.emme.assistant.adapter.out.persistence.entity;

import com.emme.assistant.domain.model.ConsentStatus;
import com.emme.kernel.type.ChannelType;
import com.emme.shared.persistence.TenantOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
    name = "channel_participant",
    uniqueConstraints = {
      @UniqueConstraint(columnNames = {"tenant_id", "channel", "provider_reference"})
    })
public class ChannelParticipantEntity extends TenantOwnedEntity {

  @Enumerated(EnumType.STRING)
  @Column(name = "channel", nullable = false, length = 20)
  private ChannelType channel;

  @Column(name = "provider_reference", nullable = false, length = 255)
  private String providerReference;

  @Column(name = "customer_id")
  private UUID customerId;

  @Enumerated(EnumType.STRING)
  @Column(name = "consent_status", nullable = false, length = 10)
  private ConsentStatus consentStatus = ConsentStatus.UNKNOWN;

  protected ChannelParticipantEntity() {}

  public ChannelParticipantEntity(UUID tenantId, ChannelType channel, String providerReference) {
    super(tenantId);
    this.channel = Objects.requireNonNull(channel, "channel must not be null");
    this.providerReference =
        Objects.requireNonNull(providerReference, "providerReference must not be null");
  }

  public ChannelType getChannel() {
    return channel;
  }

  public String getProviderReference() {
    return providerReference;
  }

  public UUID getCustomerId() {
    return customerId;
  }

  public void setCustomerId(UUID customerId) {
    this.customerId = customerId;
  }

  public ConsentStatus getConsentStatus() {
    return consentStatus;
  }

  public void setConsentStatus(ConsentStatus consentStatus) {
    this.consentStatus = consentStatus;
  }

  public void restoreIdentity(UUID id, Instant createdAt) {
    restoreAuditFields(id, createdAt, createdAt);
  }
}
