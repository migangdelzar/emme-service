package com.emme.calendar.application.port.out;

import com.emme.calendar.api.result.GoogleSpreadsheetDetails;
import java.util.List;
import java.util.UUID;

/** Port for reading spreadsheet links owned by a tenant. */
public interface GoogleSpreadsheetLinkQueryPort {
  List<GoogleSpreadsheetDetails> findByTenantId(UUID tenantId);
}
