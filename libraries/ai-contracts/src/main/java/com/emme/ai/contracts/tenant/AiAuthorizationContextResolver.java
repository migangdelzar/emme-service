package com.emme.ai.contracts.tenant;

import com.emme.kernel.context.Channel;
import java.util.Set;
import java.util.UUID;

/** Resolves tenant-scoped AI authorization from trusted backend identity and channel data. */
@FunctionalInterface
public interface AiAuthorizationContextResolver {

  AiAuthorizationContext resolve(
      UUID tenantId, String principalReference, Set<String> authenticatedRoles, Channel channel);

  record AiAuthorizationContext(
      Set<String> roles, Set<String> tenantCapabilities, Set<String> enabledFeatures) {
    public AiAuthorizationContext {
      roles = immutableTextSet(roles, "roles");
      tenantCapabilities = immutableTextSet(tenantCapabilities, "tenantCapabilities");
      enabledFeatures = immutableTextSet(enabledFeatures, "enabledFeatures");
    }

    private static Set<String> immutableTextSet(Set<String> values, String field) {
      if (values == null || values.stream().anyMatch(value -> value == null || value.isBlank())) {
        throw new IllegalArgumentException(field + " must not contain blank values");
      }
      return Set.copyOf(values);
    }
  }
}
