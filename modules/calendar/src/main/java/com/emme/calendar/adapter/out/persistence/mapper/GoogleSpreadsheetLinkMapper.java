package com.emme.calendar.adapter.out.persistence.mapper;

import com.emme.calendar.adapter.out.persistence.entity.GoogleSpreadsheetLinkEntity;
import com.emme.calendar.api.result.GoogleSpreadsheetInfo;
import org.springframework.stereotype.Component;

@Component
public class GoogleSpreadsheetLinkMapper {
  public GoogleSpreadsheetInfo toInfo(GoogleSpreadsheetLinkEntity entity) {
    return new GoogleSpreadsheetInfo(
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
