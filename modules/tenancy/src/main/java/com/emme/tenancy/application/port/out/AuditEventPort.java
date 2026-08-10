package com.emme.tenancy.application.port.out;

import com.emme.tenancy.domain.model.AuditRecord;

/** Application-owned port for durable tenancy audit metadata. */
public interface AuditEventPort {
  void save(AuditRecord record);
}
