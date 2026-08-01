package com.emme.calendar.adapter.in.web.controller;

import com.emme.calendar.application.port.out.GoogleSheetsExportPort;
import com.emme.calendar.application.port.out.GoogleSpreadsheetLinkQueryPort;
import com.emme.kernel.context.TenantContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/google/sheets")
@Tag(name = "Google Sheets")
public class SheetsController {

  private final GoogleSheetsExportPort exportService;
  private final GoogleSpreadsheetLinkQueryPort sheetLinks;

  public SheetsController(
      GoogleSheetsExportPort exportService, GoogleSpreadsheetLinkQueryPort sheetLinks) {
    this.exportService = exportService;
    this.sheetLinks = sheetLinks;
  }

  /** Export data to a new Google Sheet. */
  @PostMapping("/export")
  @PreAuthorize(
      "@featureFlagService.isEnabled('google_workspace') and @featureFlagService.isEnabled('google_sheets_export')")
  @Operation(summary = "Export data to Google Sheets")
  public ResponseEntity<Object> export(@RequestBody ExportRequest request) {
    UUID tenantId = TenantContextHolder.requireCurrentTenantId();
    try {
      var link = exportService.export(tenantId, request.exportType());
      return ResponseEntity.ok(link);
    } catch (Exception e) {
      return ResponseEntity.internalServerError()
          .body(Map.of("error", "Export failed: " + e.getMessage()));
    }
  }

  /** Re-export to an existing spreadsheet. */
  @PostMapping("/export/{spreadsheetId}")
  @PreAuthorize(
      "@featureFlagService.isEnabled('google_workspace') and @featureFlagService.isEnabled('google_sheets_export')")
  @Operation(summary = "Re-export to existing spreadsheet")
  public ResponseEntity<Object> reExport(@PathVariable String spreadsheetId) {
    UUID tenantId = TenantContextHolder.requireCurrentTenantId();
    try {
      var link = exportService.reExport(tenantId, spreadsheetId);
      return ResponseEntity.ok(link);
    } catch (Exception e) {
      return ResponseEntity.internalServerError()
          .body(Map.of("error", "Re-export failed: " + e.getMessage()));
    }
  }

  /** List spreadsheets for the tenant. */
  @GetMapping("/spreadsheets")
  @PreAuthorize(
      "@featureFlagService.isEnabled('google_workspace') and @featureFlagService.isEnabled('google_sheets_export')")
  @Operation(summary = "List exported spreadsheets")
  public ResponseEntity<Object> list() {
    UUID tenantId = TenantContextHolder.requireCurrentTenantId();
    return ResponseEntity.ok(sheetLinks.findByTenantId(tenantId));
  }

  public record ExportRequest(String exportType) {}
}
