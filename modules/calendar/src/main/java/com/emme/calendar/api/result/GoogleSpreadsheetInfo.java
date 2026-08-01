package com.emme.calendar.api.result;

import java.time.Instant;
import java.util.UUID;

/** Public read model for a spreadsheet exported by Calendar. */
public record GoogleSpreadsheetInfo(
    UUID id,
    UUID tenantId,
    String spreadsheetId,
    String spreadsheetUrl,
    String exportType,
    Instant lastExportedAt,
    Instant createdAt,
    Instant updatedAt) {}
