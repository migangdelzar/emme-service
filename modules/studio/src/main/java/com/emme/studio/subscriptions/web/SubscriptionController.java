package com.emme.studio.subscriptions.web;

import com.emme.studio.subscriptions.api.PlanType;
import com.emme.studio.subscriptions.application.SubscriptionService;
import com.emme.studio.subscriptions.entity.Subscription;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/subscriptions")
@Tag(name = "Subscriptions")
public class SubscriptionController {

  private final SubscriptionService service;

  public SubscriptionController(SubscriptionService service) {
    this.service = service;
  }

  @PostMapping
  @Operation(summary = "Create a subscription for a tenant")
  public ResponseEntity<SubscriptionResponse> create(
      @Valid @RequestBody CreateSubscriptionRequest request) {
    Subscription sub = service.create(request.tenantId(), request.planType());
    var location = java.net.URI.create("/api/v1/subscriptions/" + sub.getTenantId());
    return ResponseEntity.created(location).body(SubscriptionResponse.from(sub));
  }

  @GetMapping("/{tenantId}")
  @Operation(summary = "Get subscription for a tenant")
  public ResponseEntity<SubscriptionResponse> get(@PathVariable UUID tenantId) {
    return service
        .getForTenant(tenantId)
        .map(sub -> ResponseEntity.ok(SubscriptionResponse.from(sub)))
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping("/{tenantId}/enforce")
  @Operation(summary = "Check an entitlement for a tenant")
  public ResponseEntity<Void> enforce(
      @PathVariable UUID tenantId, @Valid @RequestBody EnforceRequest request) {
    service.enforce(tenantId, request.entitlement());
    return ResponseEntity.ok().build();
  }

  @PutMapping("/{tenantId}/plan")
  @Operation(summary = "Change the subscription plan for a tenant")
  public ResponseEntity<SubscriptionResponse> changePlan(
      @PathVariable UUID tenantId, @Valid @RequestBody ChangePlanRequest request) {
    Subscription sub = service.changePlan(tenantId, request.planType());
    return ResponseEntity.ok(SubscriptionResponse.from(sub));
  }

  // --- DTOs ---

  record CreateSubscriptionRequest(@NotNull UUID tenantId, @NotBlank String plan) {
    PlanType planType() {
      return PlanType.valueOf(plan.toUpperCase());
    }
  }

  record EnforceRequest(@NotBlank String entitlement) {}

  record ChangePlanRequest(@NotNull String plan) {
    PlanType planType() {
      return PlanType.valueOf(plan.toUpperCase());
    }
  }

  record SubscriptionResponse(
      UUID id, UUID tenantId, String plan, String status, Instant periodEndsAt, Instant createdAt) {

    static SubscriptionResponse from(Subscription s) {
      return new SubscriptionResponse(
          s.getId(),
          s.getTenantId(),
          s.getPlan().name(),
          s.getStatus().name(),
          s.getPeriodEndsAt(),
          s.getCreatedAt());
    }
  }
}
