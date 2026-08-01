package com.emme.tenancy.application.service;

import com.emme.tenancy.application.port.out.AuditEventPort;
import com.emme.tenancy.domain.model.AuditOutcome;
import com.emme.tenancy.domain.model.AuditRecord;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Records one tenant audit event in an independent transaction. */
@Service
public class RecordAuditEventService {

  private final AuditEventPort auditEventPort;

  public RecordAuditEventService(AuditEventPort auditEventPort) {
    this.auditEventPort = auditEventPort;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void record(UUID tenantId, String actorReference, String action, AuditOutcome outcome) {
    auditEventPort.save(new AuditRecord(tenantId, actorReference, action, outcome));
  }
}
