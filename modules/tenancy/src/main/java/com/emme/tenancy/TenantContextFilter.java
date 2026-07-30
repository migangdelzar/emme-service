package com.emme.tenancy;

import com.emme.kernel.context.TenantContext;
import com.emme.kernel.tracing.CorrelationId;
import com.emme.tenancy.entity.TenantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Tenant resolution order: 1. JWT tenant_id claim (secure, authenticated users) 2. ?tenant= query
 * parameter (dev convenience) 3. Hostname subdomain (production, pre-login)
 */
@Component
public class TenantContextFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(TenantContextFilter.class);
  private final TenantRepository tenantRepository;

  public TenantContextFilter(@Lazy TenantRepository tenantRepository) {
    this.tenantRepository = tenantRepository;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String correlationId = CorrelationId.generate();
    CorrelationId.set(correlationId);
    MDC.put("correlationId", correlationId);

    try {
      UUID tenantId = null;

      // 1. JWT claim (secure, authenticated users)
      var authentication = SecurityContextHolder.getContext().getAuthentication();
      tenantId = TrustedTenantResolver.fromAuthentication(authentication);

      // 2. Query parameter (dev convenience)
      if (tenantId == null) {
        tenantId =
            TrustedTenantResolver.fromQueryParam(request.getParameter("tenant"), tenantRepository);
        if (tenantId != null) log.debug("Tenant from ?tenant= param: {}", tenantId);
      }

      // 3. Hostname subdomain (production, pre-login)
      if (tenantId == null) {
        tenantId = TrustedTenantResolver.fromHost(request.getServerName(), tenantRepository);
        if (tenantId != null) log.debug("Tenant from hostname: {}", tenantId);
      }

      if (tenantId != null) {
        TenantContext.setCurrentTenant(tenantId);
        MDC.put("tenantId", tenantId.toString());
      }

      // Resolve and store databaseId (null = use default database)
      UUID databaseId = null;
      try {
        databaseId = TrustedTenantResolver.resolveDatabaseId(tenantId, tenantRepository);
      } catch (Exception e) {
        log.warn("Failed to resolve databaseId for tenant {}, using default pool", tenantId, e);
      }
      TenantContext.setCurrentDatabaseId(databaseId);

      filterChain.doFilter(request, response);
    } finally {
      TenantContext.clear();
      CorrelationId.clear();
      MDC.clear();
    }
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path.startsWith("/actuator")
        || path.startsWith("/api-docs")
        || path.startsWith("/swagger-ui");
  }
}
