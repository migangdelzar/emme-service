package com.emme.studio.adapter.out.persistence.entity;

import com.emme.shared.persistence.TenantOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "business_profile")
public class BusinessProfileEntity extends TenantOwnedEntity {

  @Column(name = "time_zone", nullable = false, length = 50)
  private String timeZone;

  @Column(name = "locale", nullable = false, length = 10)
  private String locale = "es-MX";

  @Column(name = "display_name", length = 150)
  private String displayName;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metadata")
  private String metadata;

  protected BusinessProfileEntity() {}

  public BusinessProfileEntity(UUID tenantId, String timeZone, String locale, String displayName) {
    super(tenantId);
    this.timeZone = Objects.requireNonNull(timeZone, "timeZone must not be null");
    this.locale = Objects.requireNonNull(locale, "locale must not be null");
    this.displayName = displayName;
  }

  public String getTimeZone() {
    return timeZone;
  }

  public void setTimeZone(String timeZone) {
    this.timeZone = timeZone;
  }

  public String getLocale() {
    return locale;
  }

  public void setLocale(String locale) {
    this.locale = locale;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getMetadata() {
    return metadata;
  }

  public void setMetadata(String metadata) {
    this.metadata = metadata;
  }
}
