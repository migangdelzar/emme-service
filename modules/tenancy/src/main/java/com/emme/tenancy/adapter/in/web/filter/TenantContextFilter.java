package com.emme.tenancy.adapter.in.web.filter;

import com.emme.kernel.context.TenantContext;
import com.emme.kernel.context.TenantContextBridge;
import com.emme.kernel.context.TenantExecutionContext;
import com.emme.kernel.context.TenantExecutionContextScope;
import com.emme.kernel.tracing.CorrelationId;
import com.emme.tenancy.application.port.out.TenantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
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
      var authenticationTenant =
          TrustedTenantResolver.fromAuthentication(authentication, tenantRepository);
      tenantId = authenticationTenant.tenantId();

      if (authenticationTenant.tenantBound() && tenantId == null) {
        throw new AccessDeniedException("Authenticated tenant could not be resolved");
      }

      UUID selectedTenantId = resolveRequestTenantSelectors(request, tenantId);
      if (tenantId == null) {
        tenantId = selectedTenantId;
      }

      if (tenantId != null) {
        TenantContext.setCurrentTenant(tenantId);
        MDC.put("tenantId", tenantId.toString());
      }

      UUID databaseId = TrustedTenantResolver.resolveDatabaseId(tenantId, tenantRepository);
      TenantContext.setCurrentDatabaseId(databaseId);

      if (tenantId == null) {
        filterChain.doFilter(request, response);
      } else {
        TenantExecutionContext context =
            new TenantExecutionContext(tenantId, databaseId, correlationId);
        try {
          TenantExecutionContextScope.run(
              context,
              () -> TenantContextBridge.runCurrent(() -> filterChain.doFilter(request, response)));
        } catch (RuntimeException exception) {
          rethrowFilterException(exception);
        }
      }
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

  private static void rethrowFilterException(RuntimeException exception)
      throws IOException, ServletException {
    Throwable cause = exception;
    while (cause instanceof RuntimeException && cause.getCause() != null) {
      cause = cause.getCause();
    }
    if (cause instanceof IOException ioException) {
      throw ioException;
    }
    if (cause instanceof ServletException servletException) {
      throw servletException;
    }
    throw exception;
  }

  private UUID resolveRequestTenantSelectors(
      HttpServletRequest request, UUID authenticatedTenantId) {
    List<UUID> selectedTenantIds = new ArrayList<>();
    addTenantSelector(selectedTenantIds, request.getHeader("X-Tenant-Slug"));
    addTenantSelector(selectedTenantIds, request.getHeader("X-Emme-Tenant-Slug"));
    addTenantSelector(selectedTenantIds, request.getParameter("tenant"));

    String hostSlug = TrustedTenantResolver.slugFromHost(request.getServerName());
    if (hostSlug != null) {
      addTenantSelector(selectedTenantIds, hostSlug);
    }

    if (selectedTenantIds.stream().distinct().count() > 1) {
      throw new AccessDeniedException("Tenant selectors disagree");
    }

    UUID selectedTenantId = selectedTenantIds.isEmpty() ? null : selectedTenantIds.getFirst();
    if (authenticatedTenantId != null
        && selectedTenantId != null
        && !authenticatedTenantId.equals(selectedTenantId)) {
      throw new AccessDeniedException("Tenant selector conflicts with authenticated tenant");
    }
    return selectedTenantId;
  }

  private void addTenantSelector(List<UUID> selectedTenantIds, String selector) {
    if (selector == null || selector.isBlank()) {
      return;
    }
    UUID tenantId = TrustedTenantResolver.fromQueryParam(selector, tenantRepository);
    if (tenantId == null) {
      throw new AccessDeniedException("Unknown tenant selector");
    }
    selectedTenantIds.add(tenantId);
  }
}
