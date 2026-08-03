package com.emme.tenancy.adapter.out.persistence.repository;

import com.emme.tenancy.adapter.out.persistence.entity.AuditEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
  List<AuditEvent> findByTenantIdOrderByOccurredAtDesc(UUID tenantId);

  List<AuditEvent> findByActorReferenceOrderByOccurredAtDesc(String actorReference);
}
