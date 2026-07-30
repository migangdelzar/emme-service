package com.emme.assistant.ai.application;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class ToolRegistry {

  private final Map<String, ToolExecutor> tools = new ConcurrentHashMap<>();
  private volatile boolean initialized;

  @PostConstruct
  void init() {
    ensureInitialized();
  }

  /** Public for test access. Idempotent. */
  public void ensureInitialized() {
    if (initialized) return;
    synchronized (this) {
      if (initialized) return;

      registerTool(
          "searchCatalog",
          params -> {
            String tenantId = params.getOrDefault("tenantId", "unknown");
            String query = params.getOrDefault("query", "");
            return "MOCK: Found 3 services matching '" + query + "' for tenant " + tenantId;
          });

      registerTool(
          "getAvailability",
          params -> {
            String tenantId = params.getOrDefault("tenantId", "unknown");
            String serviceId = params.getOrDefault("serviceId", "unknown");
            String date = params.getOrDefault("date", "today");
            return "MOCK: Available slots on "
                + date
                + " for service "
                + serviceId
                + " (tenant "
                + tenantId
                + ")";
          });

      registerTool(
          "estimatePrice",
          params -> {
            String tenantId = params.getOrDefault("tenantId", "unknown");
            String serviceId = params.getOrDefault("serviceId", "unknown");
            return "MOCK: Estimated price: $50 for service "
                + serviceId
                + " (tenant "
                + tenantId
                + ")";
          });

      initialized = true;
    }
  }

  public void registerTool(String name, ToolExecutor executor) {
    tools.put(name, executor);
  }

  public String executeTool(String name, Map<String, String> params) {
    ToolExecutor executor = tools.get(name);
    if (executor == null) {
      return "MOCK: Tool '"
          + name
          + "' not found. Available tools: "
          + String.join(", ", tools.keySet());
    }
    try {
      return executor.execute(params);
    } catch (Exception e) {
      return "MOCK: Tool execution failed for '" + name + "': " + e.getMessage();
    }
  }
}
