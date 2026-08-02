package com.emme.calendar.adapter.out.persistence.entity;

import com.emme.shared.persistence.TenantOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "google_spreadsheet_link")
public class GoogleSpreadsheetLinkEntity extends TenantOwnedEntity {

  @Column(name = "spreadsheet_id", nullable = false)
  private String spreadsheetId;

  @Column(name = "spreadsheet_url", nullable = false)
  private String spreadsheetUrl;

  @Column(name = "export_type", nullable = false)
  private String exportType;

  @Column(name = "last_exported_at")
  private Instant lastExportedAt;

  protected GoogleSpreadsheetLinkEntity() {}

  public GoogleSpreadsheetLinkEntity(
      UUID tenantId, String spreadsheetId, String spreadsheetUrl, String exportType) {
    super(tenantId);
    this.spreadsheetId = Objects.requireNonNull(spreadsheetId, "spreadsheetId must not be null");
    this.spreadsheetUrl = Objects.requireNonNull(spreadsheetUrl, "spreadsheetUrl must not be null");
    this.exportType = Objects.requireNonNull(exportType, "exportType must not be null");
  }

  public String getSpreadsheetId() {
    return spreadsheetId;
  }

  public String getSpreadsheetUrl() {
    return spreadsheetUrl;
  }

  public void setSpreadsheetUrl(String spreadsheetUrl) {
    this.spreadsheetUrl = spreadsheetUrl;
  }

  public String getExportType() {
    return exportType;
  }

  public void setExportType(String exportType) {
    this.exportType = exportType;
  }

  public Instant getLastExportedAt() {
    return lastExportedAt;
  }

  public void setLastExportedAt(Instant lastExportedAt) {
    this.lastExportedAt = lastExportedAt;
  }
}
