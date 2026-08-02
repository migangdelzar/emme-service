package com.emme.studio.adapter.out.persistence.entity;

import com.emme.kernel.type.NotificationChannel;
import com.emme.shared.persistence.TenantOwnedEntity;
import com.emme.studio.domain.model.TemplatePolicy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "notification_preference")
public class NotificationPreferenceEntity extends TenantOwnedEntity {

  @Enumerated(EnumType.STRING)
  @Column(name = "channel", nullable = false, length = 30)
  private NotificationChannel channel;

  @Column(name = "enabled", nullable = false)
  private boolean enabled = true;

  @Enumerated(EnumType.STRING)
  @Column(name = "template_policy", nullable = false, length = 40)
  private TemplatePolicy templatePolicy = TemplatePolicy.DEFAULT;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metadata")
  private String metadata;

  protected NotificationPreferenceEntity() {}

  public NotificationPreferenceEntity(UUID tenantId, NotificationChannel channel) {
    super(tenantId);
    this.channel = Objects.requireNonNull(channel, "channel must not be null");
  }

  public NotificationChannel getChannel() {
    return channel;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public TemplatePolicy getTemplatePolicy() {
    return templatePolicy;
  }

  public void setTemplatePolicy(TemplatePolicy templatePolicy) {
    this.templatePolicy = templatePolicy;
  }

  public String getMetadata() {
    return metadata;
  }

  public void setMetadata(String metadata) {
    this.metadata = metadata;
  }
}
