package com.emme.calendar.adapter.out.persistence.repository;

import com.emme.calendar.adapter.out.persistence.entity.GoogleSpreadsheetLinkEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataGoogleSpreadsheetLinkRepository
    extends JpaRepository<GoogleSpreadsheetLinkEntity, UUID> {

  Optional<GoogleSpreadsheetLinkEntity> findByTenantIdAndSpreadsheetId(
      UUID tenantId, String spreadsheetId);
}
