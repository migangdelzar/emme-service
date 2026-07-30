package com.emme.calendar.infrastructure.google.entity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GoogleSpreadsheetLinkRepository
    extends JpaRepository<GoogleSpreadsheetLink, UUID> {

  List<GoogleSpreadsheetLink> findByTenantId(UUID tenantId);

  Optional<GoogleSpreadsheetLink> findByTenantIdAndSpreadsheetId(
      UUID tenantId, String spreadsheetId);
}
