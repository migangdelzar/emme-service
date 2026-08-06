package com.emme.tenancy.adapter.in.web.filter;

import com.emme.kernel.context.TenantContext;
import com.emme.kernel.tracing.CorrelationId;
import com.emme.tenancy.application.port.out.TenantRepository;
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

/** Resolves trusted tenant context before an HTTP request reaches application use cases. */
@Component
public class TenantContextFilter extends OncePerRequestFilter {

  public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

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
    response.setHeader(CORRELATION_ID_HEADER, correlationId);

    try {
      UUID tenantId = null;

      var authentication = SecurityContextHolder.getContext().getAuthentication();
      tenantId = TrustedTenantResolver.fromAuthentication(authentication);

      if (tenantId == null) {
        // Try X-Tenant-Slug header (or X-Emme-Tenant-Slug for E2E tests)
        String headerSlug = request.getHeader("X-Tenant-Slug");
        if (headerSlug == null || headerSlug.isBlank()) {
          headerSlug = request.getHeader("X-Emme-Tenant-Slug");
        }
        if (headerSlug != null && !headerSlug.isBlank()) {
          tenantId = TrustedTenantResolver.fromQueryParam(headerSlug.trim(), tenantRepository);
          if (tenantId != null) log.debug("Tenant from X-Tenant-Slug header: {}", tenantId);
        }
      }

      if (tenantId == null) {
        tenantId =
            TrustedTenantResolver.fromQueryParam(request.getParameter("tenant"), tenantRepository);
        if (tenantId != null) log.debug("Tenant from ?tenant= param: {}", tenantId);
      }

      if (tenantId == null) {
        tenantId = TrustedTenantResolver.fromHost(request.getServerName(), tenantRepository);
        if (tenantId != null) log.debug("Tenant from hostname: {}", tenantId);
      }

      if (tenantId != null) {
        TenantContext.setCurrentTenant(tenantId);
        MDC.put("tenantId", tenantId.toString());
      }

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
