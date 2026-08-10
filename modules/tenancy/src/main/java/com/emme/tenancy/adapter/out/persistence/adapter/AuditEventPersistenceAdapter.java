package com.emme.tenancy.adapter.out.persistence.adapter;

import com.emme.tenancy.adapter.out.persistence.entity.AuditEvent;
import com.emme.tenancy.adapter.out.persistence.repository.AuditEventRepository;
import com.emme.tenancy.application.port.out.AuditEventPort;
import com.emme.tenancy.domain.model.AuditRecord;
import org.springframework.stereotype.Component;

@Component
public class AuditEventPersistenceAdapter implements AuditEventPort {
  private final AuditEventRepository repository;

  public AuditEventPersistenceAdapter(AuditEventRepository repository) {
    this.repository = repository;
  }

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
