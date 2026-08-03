package com.emme.calendar.application.port.out;

import com.emme.calendar.api.result.GoogleSpreadsheetInfo;
import java.util.UUID;

/** Port for exporting tenant data through an external spreadsheet provider. */
public interface GoogleSheetsExportPort {
  GoogleSpreadsheetInfo export(UUID tenantId, String exportType) throws Exception;

  GoogleSpreadsheetInfo reExport(UUID tenantId, String spreadsheetId) throws Exception;
}
