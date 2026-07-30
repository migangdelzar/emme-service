package com.emme.tenancy;

import com.emme.kernel.context.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import java.util.UUID;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Sets PostgreSQL runtime parameter {@code app.current_tenant_id} before every JPA repository call,
 * enabling RLS policies.
 *
 * <p>Disabled when datasource URL contains "h2" to support in-memory H2 tests (H2 does not support
 * {@code SET LOCAL}).
 *
 * <p>RLS policy: {@code USING (current_setting('app.current_tenant_id')::uuid = tenant_id)}
 */
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
    if (tenantId != null) {
      String literalTenantId = tenantId.toString();

      // SET LOCAL requires a literal value, not a parameter placeholder.
      // PostgreSQL rejects parameterised SET LOCAL statements.
      entityManager
          .createNativeQuery("SET LOCAL app.current_tenant_id = '" + literalTenantId + "'")
          .executeUpdate();

      // Look up schema_name from tenant registry and set search_path
      try {
        String schemaName =
            (String)
                entityManager
                    .createNativeQuery(
                        "SELECT schema_name FROM emme_core.tenant_registry WHERE tenant_id = '"
                            + literalTenantId
                            + "'")
                    .getSingleResult();
        entityManager
            .createNativeQuery("SET LOCAL search_path TO " + schemaName + ", emme_core, public")
            .executeUpdate();
      } catch (NoResultException e) {
        // Tenant not found in registry – RLS is set, but no schema override
      }
    }
  }
}
