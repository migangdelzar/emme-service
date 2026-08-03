package com.emme.tenancy.adapter.in.web.filter;

import com.emme.tenancy.application.port.out.TenantRepository;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

/** Resolves tenant identity only from trusted authentication or server-controlled sources. */
public final class TrustedTenantResolver {

  private TrustedTenantResolver() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static UUID fromAuthentication(Authentication authentication) {
    if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
      return null;
    }
    String tenantClaim = jwt.getClaimAsString("tenant_id");
    if (tenantClaim == null || tenantClaim.isBlank()) {
      return null;
    }
    try {
      return UUID.fromString(tenantClaim);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /** Extracts a tenant slug from a trusted hostname subdomain. */
  public static String slugFromHost(String host) {
    if (host == null || host.isBlank()) return null;

    int portIdx = host.indexOf(':');
    if (portIdx > 0) host = host.substring(0, portIdx);

    if ("localhost".equals(host) || "127.0.0.1".equals(host)) return null;

    if (host.endsWith(".lvh.me")) {
      String[] parts = host.split("\\.");
      return parts.length >= 3 ? parts[0] : null;
    }

    if (host.endsWith(".emme.app") || host.endsWith(".emme.local")) {
      String[] parts = host.split("\\.");
      return parts.length >= 3 ? parts[0] : null;
    }

    String[] parts = host.split("\\.");
    return parts.length > 2 ? parts[0] : null;
  }

  public static UUID fromHost(String host, TenantRepository tenantRepository) {
    String slug = slugFromHost(host);
    if (slug == null) return null;
    return tenantRepository.findBySlug(slug).map(tenant -> tenant.id()).orElse(null);
  }

  /** Resolves a tenant from the development-only query parameter. */
  public static UUID fromQueryParam(String tenantParam, TenantRepository tenantRepository) {
    if (tenantParam == null || tenantParam.isBlank()) return null;
    return tenantRepository.findBySlug(tenantParam.trim()).map(tenant -> tenant.id()).orElse(null);
  }

  /** Resolves the dedicated database ID or null for the default database. */
  public static UUID resolveDatabaseId(UUID tenantId, TenantRepository tenantRepository) {
    if (tenantId == null) return null;
    return tenantRepository.findDatabaseIdByTenantId(tenantId).orElse(null);
  }
}
