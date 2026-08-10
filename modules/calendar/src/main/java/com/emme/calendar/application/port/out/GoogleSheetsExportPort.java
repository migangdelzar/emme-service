package com.emme.calendar.application.port.out;

import com.emme.calendar.api.result.GoogleSpreadsheetDetails;
import java.util.UUID;

/** Port for exporting tenant data through an external spreadsheet provider. */
public interface GoogleSheetsExportPort {
  GoogleSpreadsheetDetails export(UUID tenantId, String exportType) throws Exception;

  GoogleSpreadsheetDetails reExport(UUID tenantId, String spreadsheetId) throws Exception;
}
