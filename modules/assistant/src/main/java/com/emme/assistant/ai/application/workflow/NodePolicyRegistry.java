package com.emme.assistant.ai.application.workflow;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Immutable registry that binds one explicit policy to each workflow node identifier. */
public final class NodePolicyRegistry {

  private final Map<String, NodeProfile> profiles;

  public NodePolicyRegistry(Collection<NodeProfile> profiles) {
    Objects.requireNonNull(profiles, "profiles must not be null");
    Map<String, NodeProfile> registered = new LinkedHashMap<>();
    for (NodeProfile profile : profiles) {
      Objects.requireNonNull(profile, "profiles must not contain null values");
      if (registered.putIfAbsent(profile.nodeId(), profile) != null) {
        throw new IllegalArgumentException("Duplicate node policy: " + profile.nodeId());
      }
    }
    this.profiles = Map.copyOf(registered);
  }

  public NodeProfile profile(String nodeId) {
    Objects.requireNonNull(nodeId, "nodeId must not be null");
    NodeProfile profile = profiles.get(nodeId);
    if (profile == null) {
      throw new IllegalArgumentException("Unknown workflow node: " + nodeId);
    }
    return profile;
  }

  public Set<String> nodeIds() {
    return profiles.keySet();
  }

  public Map<String, NodeProfile> profiles() {
    return profiles;
  }
}
