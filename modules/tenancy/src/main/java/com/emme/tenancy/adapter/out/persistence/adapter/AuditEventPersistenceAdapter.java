package com.emme.tenancy.adapter.out.persistence.adapter;

import com.emme.tenancy.adapter.out.persistence.entity.AuditEvent;
import com.emme.tenancy.adapter.out.persistence.repository.AuditEventRepository;
import com.emme.tenancy.application.port.out.AuditEventPort;
import com.emme.tenancy.domain.model.AuditRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditEventPersistenceAdapter implements AuditEventPort {
  private final AuditEventRepository repository;

  @Override
  public void save(AuditRecord record) {
    repository.save(
        new AuditEvent(
            record.tenantId(),
            record.actorReference(),
            record.action(),
            AuditEvent.AuditOutcome.valueOf(record.outcome().name())));
  }
}
