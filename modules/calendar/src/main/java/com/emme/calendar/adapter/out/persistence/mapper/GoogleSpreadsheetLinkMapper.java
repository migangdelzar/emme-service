package com.emme.calendar.adapter.out.persistence.mapper;

import com.emme.calendar.adapter.out.persistence.entity.GoogleSpreadsheetLinkEntity;
import com.emme.calendar.api.result.GoogleSpreadsheetDetails;
import org.springframework.stereotype.Component;

@Component
public class GoogleSpreadsheetLinkMapper {
  public GoogleSpreadsheetDetails toResult(GoogleSpreadsheetLinkEntity entity) {
    return new GoogleSpreadsheetDetails(
        entity.getId(),
        entity.getTenantId(),
        entity.getSpreadsheetId(),
        entity.getSpreadsheetUrl(),
        entity.getExportType(),
        entity.getLastExportedAt(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
