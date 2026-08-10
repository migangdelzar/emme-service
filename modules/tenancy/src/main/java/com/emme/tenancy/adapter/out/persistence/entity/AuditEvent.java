package com.emme.tenancy.adapter.out.persistence.entity;

import com.emme.shared.persistence.PersistedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "platform_audit_event", schema = "emme_core")
public class AuditEvent extends PersistedEntity {

  @Column(name = "tenant_id")
  private UUID tenantId;

  @Column(name = "actor_reference", nullable = false, length = 150)
  private String actorReference;

  @Column(nullable = false, length = 120)
  private String action;

  @Column(nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  private AuditOutcome outcome;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  protected AuditEvent() {}

  public AuditEvent(UUID tenantId, String actorReference, String action, AuditOutcome outcome) {
    this.tenantId = tenantId;
    this.actorReference = actorReference;
    this.action = action;
    this.outcome = outcome;
    this.occurredAt = Instant.now();
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public String getActorReference() {
    return actorReference;
  }

  public String getAction() {
    return action;
  }

  public AuditOutcome getOutcome() {
    return outcome;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public enum AuditOutcome {
    SUCCEEDED,
    DENIED,
    FAILED
  }
}
