package com.emme.tenancy.application.audit;

import com.emme.tenancy.application.port.out.AuditEventPort;
import com.emme.tenancy.domain.model.AuditOutcome;
import com.emme.tenancy.domain.model.AuditRecord;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Records one tenant audit event in an independent transaction. */
@Service
public class AuditEventRecorder {

  private final AuditEventPort auditEventPort;

  public AuditEventRecorder(AuditEventPort auditEventPort) {
    this.auditEventPort = auditEventPort;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void record(UUID tenantId, String actorReference, String action, AuditOutcome outcome) {
    auditEventPort.save(new AuditRecord(tenantId, actorReference, action, outcome));
  }
}
