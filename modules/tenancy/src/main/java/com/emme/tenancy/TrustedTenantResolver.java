package com.emme.tenancy;

import com.emme.tenancy.adapter.out.persistence.repository.TenantRepository;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Resolves tenant ID from trusted sources only. Client-supplied tenant IDs are NEVER trusted
 * directly.
 *
 * <p>Resolution order: 1. JWT tenant_id claim (set by Keycloak) 2. Hostname subdomain → slug lookup
 * (pre-login context, e.g. studio-a.emme.app)
 */
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

  /**
   * Extracts tenant slug from hostname subdomain. Production: studio-a.emme.app → "studio-a" Local
   * dev: studio-a.lvh.me:3000 → "studio-a" Localhost: returns null
   */
  public static String slugFromHost(String host) {
    if (host == null || host.isBlank()) return null;

    // Strip port
    int portIdx = host.indexOf(':');
    if (portIdx > 0) host = host.substring(0, portIdx);

    // localhost → no tenant
    if ("localhost".equals(host) || "127.0.0.1".equals(host)) return null;

    // lvh.me wildcard DNS
    if (host.endsWith(".lvh.me")) {
      String[] parts = host.split("\\.");
      return parts.length >= 3 ? parts[0] : null;
    }

    // Production domains
    if (host.endsWith(".emme.app") || host.endsWith(".emme.local")) {
      String[] parts = host.split("\\.");
      return parts.length >= 3 ? parts[0] : null;
    }

    // Generic: first subdomain
    String[] parts = host.split("\\.");
    return parts.length > 2 ? parts[0] : null;
  }

  public static UUID fromHost(String host, TenantRepository tenantRepository) {
    String slug = slugFromHost(host);
    if (slug == null) return null;
    return tenantRepository.findBySlug(slug).map(t -> t.getId()).orElse(null);
  }

  /** Resolves tenant from query parameter (dev convenience). e.g. ?tenant=studio-a → UUID lookup */
  public static UUID fromQueryParam(String tenantParam, TenantRepository tenantRepository) {
    if (tenantParam == null || tenantParam.isBlank()) return null;
    return tenantRepository.findBySlug(tenantParam.trim()).map(t -> t.getId()).orElse(null);
  }

  /**
   * Resolves the database ID for a given tenant ID. Returns null if the tenant has no dedicated
   * database (use default).
   */
  public static UUID resolveDatabaseId(UUID tenantId, TenantRepository tenantRepository) {
    if (tenantId == null) return null;
    return tenantRepository.findDatabaseIdByTenantId(tenantId).orElse(null);
  }
}
