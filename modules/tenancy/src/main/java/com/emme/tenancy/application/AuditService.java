package com.emme.tenancy.application;

import com.emme.tenancy.adapter.out.persistence.entity.AuditEvent;
import com.emme.tenancy.adapter.out.persistence.entity.AuditEvent.AuditOutcome;
import com.emme.tenancy.adapter.out.persistence.repository.AuditEventRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

  private final AuditEventRepository repository;

  public AuditService(AuditEventRepository repository) {
    this.repository = repository;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void record(UUID tenantId, String actorReference, String action, AuditOutcome outcome) {
    repository.save(new AuditEvent(tenantId, actorReference, action, outcome));
  }

  public void succeeded(UUID tenantId, String actor, String action) {
    record(tenantId, actor, action, AuditOutcome.SUCCEEDED);
  }

  public void denied(UUID tenantId, String actor, String action) {
    record(tenantId, actor, action, AuditOutcome.DENIED);
  }

  public void failed(UUID tenantId, String actor, String action) {
    record(tenantId, actor, action, AuditOutcome.FAILED);
  }
}
