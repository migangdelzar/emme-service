package com.emme.tenancy.adapter.out.client.database;

import com.emme.kernel.context.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import java.util.UUID;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/** Applies tenant RLS and schema settings before application-owned JPA repository calls. */
@Aspect
@Component
@ConditionalOnExpression(
    "!'${spring.datasource.url:}'.contains('h2') && ${emme.tenancy.rls-enabled:true}")
public class TenantContextAspect {

  private final EntityManager entityManager;

  public TenantContextAspect(@Lazy EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Before("execution(* com.emme..*Repository.*(..)) && !within(com.emme.tenancy..*)")
  public void setTenantContext() {
    UUID tenantId = TenantContext.getCurrentTenantId();
    if (tenantId == null) {
      return;
    }

    String literalTenantId = tenantId.toString();
    entityManager
        .createNativeQuery("SET LOCAL app.current_tenant_id = '" + literalTenantId + "'")
        .executeUpdate();

    try {
      String schemaName =
          (String)
              entityManager
                  .createNativeQuery(
                      "SELECT schema_name FROM emme_core.tenant_registry WHERE tenant_id = '"
                          + literalTenantId
                          + "'")
                  .getSingleResult();
      String validatedSchemaName = TenantSchemaName.requireValid(schemaName);
      entityManager
          .createNativeQuery(
              "SET LOCAL search_path TO " + validatedSchemaName + ", emme_core, public")
          .executeUpdate();
    } catch (NoResultException ignored) {
      // Tenant not found in registry: RLS remains active without a schema override.
    }
  }
}
