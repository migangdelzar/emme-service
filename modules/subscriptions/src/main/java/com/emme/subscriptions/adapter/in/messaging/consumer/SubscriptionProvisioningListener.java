package com.emme.subscriptions.adapter.in.messaging.consumer;

import com.emme.tenancy.api.event.TenantActivated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(name = "bootstrapJdbcTemplate")
public class SubscriptionProvisioningListener {

  private static final Logger log = LoggerFactory.getLogger(SubscriptionProvisioningListener.class);
  private final JdbcTemplate jdbc;

  public SubscriptionProvisioningListener(@Qualifier("bootstrapJdbcTemplate") JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @ApplicationModuleListener
  public void onTenantActivated(TenantActivated event) {
    try {
      jdbc.update(
          "INSERT INTO "
              + event.schemaName()
              + ".subscription "
              + "(id, tenant_id, plan, status, period_ends_at, updated_at) "
              + "VALUES (gen_random_uuid(), ?::uuid, 'PRO', 'ACTIVE', now() + interval '30 days', now())",
          event.tenantId().toString());
      log.info("Subscription PRO created for tenant {} ({})", event.tenantId(), event.slug());
    } catch (Exception e) {
      log.warn(
          "Subscription already exists or failed for {}: {}", event.schemaName(), e.getMessage());
    }
  }
}
