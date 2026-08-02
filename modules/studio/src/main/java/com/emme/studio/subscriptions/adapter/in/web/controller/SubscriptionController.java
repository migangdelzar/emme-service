package com.emme.studio.subscriptions.adapter.in.web.controller;

import static com.emme.kernel.context.TenantContextHolder.withCurrentTenant;

import com.emme.studio.subscriptions.adapter.in.web.mapper.SubscriptionWebMapper;
import com.emme.studio.subscriptions.adapter.in.web.request.ChangeSubscriptionPlanRequest;
import com.emme.studio.subscriptions.adapter.in.web.request.CreateSubscriptionRequest;
import com.emme.studio.subscriptions.adapter.in.web.request.EnforceEntitlementRequest;
import com.emme.studio.subscriptions.adapter.in.web.response.SubscriptionResponse;
import com.emme.studio.subscriptions.api.command.EnforceEntitlementCommand;
import com.emme.studio.subscriptions.api.query.GetSubscriptionQuery;
import com.emme.studio.subscriptions.api.result.SubscriptionInfo;
import com.emme.studio.subscriptions.api.usecase.ChangeSubscriptionPlanUseCase;
import com.emme.studio.subscriptions.api.usecase.CreateSubscriptionUseCase;
import com.emme.studio.subscriptions.api.usecase.EnforceEntitlementUseCase;
import com.emme.studio.subscriptions.api.usecase.GetSubscriptionUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping("/api/subscriptions")
@Tag(name = "Subscriptions")
public class SubscriptionController {

  private final CreateSubscriptionUseCase createSubscription;
  private final GetSubscriptionUseCase getSubscription;
  private final EnforceEntitlementUseCase enforceEntitlement;
  private final ChangeSubscriptionPlanUseCase changeSubscriptionPlan;
  private final SubscriptionWebMapper mapper;

  public SubscriptionController(
      CreateSubscriptionUseCase createSubscription,
      GetSubscriptionUseCase getSubscription,
      EnforceEntitlementUseCase enforceEntitlement,
      ChangeSubscriptionPlanUseCase changeSubscriptionPlan,
      SubscriptionWebMapper mapper) {
    this.createSubscription = createSubscription;
    this.getSubscription = getSubscription;
    this.enforceEntitlement = enforceEntitlement;
    this.changeSubscriptionPlan = changeSubscriptionPlan;
    this.mapper = mapper;
  }

  @PostMapping
  @Operation(summary = "Create a subscription for a tenant")
  public ResponseEntity<SubscriptionResponse> create(
      @Valid @RequestBody CreateSubscriptionRequest request) {
    return withCurrentTenant(
        tenantId -> {
          if (!tenantId.equals(request.tenantId())) {
            return ResponseEntity.notFound().build();
          }
          SubscriptionInfo subscription = createSubscription.create(request.toCommand());
          var location = java.net.URI.create("/api/subscriptions/" + subscription.tenantId());
          return ResponseEntity.created(location).body(mapper.toResponse(subscription));
        });
  }

  @GetMapping("/{tenantId}")
  @Operation(summary = "Get subscription for a tenant")
  public ResponseEntity<SubscriptionResponse> get(@PathVariable UUID tenantId) {
    return withCurrentTenant(
        currentTenant -> {
          if (!currentTenant.equals(tenantId)) {
            return ResponseEntity.notFound().build();
          }
          return getSubscription
              .get(new GetSubscriptionQuery(tenantId))
              .map(subscription -> ResponseEntity.ok(mapper.toResponse(subscription)))
              .orElse(ResponseEntity.notFound().build());
        });
  }

  @PostMapping("/{tenantId}/enforce")
  @Operation(summary = "Check an entitlement for a tenant")
  public ResponseEntity<Void> enforce(
      @PathVariable UUID tenantId, @Valid @RequestBody EnforceEntitlementRequest request) {
    return withCurrentTenant(
        currentTenant -> {
          if (!currentTenant.equals(tenantId)) {
            return ResponseEntity.notFound().build();
          }
          enforceEntitlement.enforce(
              new EnforceEntitlementCommand(tenantId, request.entitlement()));
          return ResponseEntity.ok().build();
        });
  }

  @PutMapping("/{tenantId}/plan")
  @Operation(summary = "Change the subscription plan for a tenant")
  public ResponseEntity<SubscriptionResponse> changePlan(
      @PathVariable UUID tenantId, @Valid @RequestBody ChangeSubscriptionPlanRequest request) {
    return withCurrentTenant(
        currentTenant -> {
          if (!currentTenant.equals(tenantId)) {
            return ResponseEntity.notFound().build();
          }
          SubscriptionInfo subscription =
              changeSubscriptionPlan.change(request.toCommand(tenantId));
          return ResponseEntity.ok(mapper.toResponse(subscription));
        });
  }
}
