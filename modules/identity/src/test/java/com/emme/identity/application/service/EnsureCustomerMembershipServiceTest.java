package com.emme.identity.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.identity.application.port.out.CustomerMembershipRepository;
import com.emme.identity.domain.model.CustomerMembership;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnsureCustomerMembershipServiceTest {

  @Mock private CustomerMembershipRepository repository;

  @InjectMocks private EnsureCustomerMembershipService service;

  @Test
  void createsMembershipWhenCustomerHasNoMembershipForTenant() {
    UUID customerId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    when(repository.existsByCustomerIdAndTenantId(customerId, tenantId)).thenReturn(false);

    service.ensureForCustomer(customerId, tenantId);

    ArgumentCaptor<CustomerMembership> membership =
        ArgumentCaptor.forClass(CustomerMembership.class);
    verify(repository).save(membership.capture());
    assertThat(membership.getValue().customerId()).isEqualTo(customerId);
    assertThat(membership.getValue().tenantId()).isEqualTo(tenantId);
    assertThat(membership.getValue().createdAt()).isNotNull();
  }

  @Test
  void doesNotCreateDuplicateMembership() {
    UUID customerId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    when(repository.existsByCustomerIdAndTenantId(customerId, tenantId)).thenReturn(true);

    service.ensureForCustomer(customerId, tenantId);

    verify(repository, never()).save(any(CustomerMembership.class));
  }
}
