package com.emme.identity.adapter.out.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emme.identity.api.query.GetCurrentUserMembershipsQuery;
import com.emme.identity.api.result.MembershipDetails;
import com.emme.identity.api.usecase.GetCurrentUserMembershipsUseCase;
import com.emme.identity.application.authorization.FeatureFlagEvaluator;
import com.emme.identity.application.port.out.CustomerMembershipRepository;
import com.emme.identity.application.port.out.SubscriptionPlanPort;
import com.emme.identity.domain.model.MembershipStatus;
import com.emme.kernel.context.Channel;
import com.emme.subscriptions.api.type.PlanType;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdentityAiAuthorizationContextResolverTest {

  @Test
  void intersectsWebRolesWithTheAuthenticatedUsersTenantMembership() {
    UUID tenantId = UUID.randomUUID();
    GetCurrentUserMembershipsUseCase memberships = mock(GetCurrentUserMembershipsUseCase.class);
    when(memberships.getMemberships(new GetCurrentUserMembershipsQuery("user-1")))
        .thenReturn(
            List.of(
                new MembershipDetails(
                    UUID.randomUUID(),
                    tenantId,
                    "Salon",
                    "tenant_staff",
                    MembershipStatus.ACTIVE)));
    SubscriptionPlanPort plans = mock(SubscriptionPlanPort.class);
    when(plans.findPlanForTenant(tenantId)).thenReturn(java.util.Optional.of(PlanType.PRO));
    FeatureFlagEvaluator features = mock(FeatureFlagEvaluator.class);
    when(features.getEffective(tenantId)).thenReturn(Map.of("ai_chat", true, "disabled", false));
    IdentityAiAuthorizationContextResolver resolver =
        new IdentityAiAuthorizationContextResolver(
            memberships, plans, features, mock(CustomerMembershipRepository.class));

    var result =
        resolver.resolve(
            tenantId, "user-1", Set.of("ROLE_tenant_staff", "ROLE_tenant_owner"), Channel.WEB);

    assertThat(result.roles()).containsExactly("ROLE_tenant_staff");
    assertThat(result.tenantCapabilities())
        .contains("appointments:write", "ai:basic", "service_catalog", "appointments");
    assertThat(result.enabledFeatures()).containsExactly("ai_chat");
  }

  @Test
  void onlyAuthorizesAWhatsAppClientWhoseCustomerMembershipExists() {
    UUID tenantId = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();
    CustomerMembershipRepository customerMemberships = mock(CustomerMembershipRepository.class);
    when(customerMemberships.existsByCustomerIdAndTenantId(customerId, tenantId)).thenReturn(true);
    FeatureFlagEvaluator features = mock(FeatureFlagEvaluator.class);
    when(features.getEffective(tenantId)).thenReturn(Map.of());
    IdentityAiAuthorizationContextResolver resolver =
        new IdentityAiAuthorizationContextResolver(
            mock(GetCurrentUserMembershipsUseCase.class),
            tenant -> java.util.Optional.of(PlanType.STARTER),
            features,
            customerMemberships);

    var result =
        resolver.resolve(tenantId, customerId.toString(), Set.of("client"), Channel.WHATSAPP);

    assertThat(result.roles()).containsExactly("client");
    assertThat(result.tenantCapabilities()).contains("appointments:read");
  }
}
