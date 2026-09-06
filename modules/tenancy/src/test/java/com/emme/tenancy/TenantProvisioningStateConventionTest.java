package com.emme.tenancy;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.tenancy.adapter.out.persistence.entity.TenantRegistryEntity;
import com.emme.tenancy.api.type.TenantProvisioningState;
import com.emme.tenancy.api.type.TenantStatus;
import com.emme.tenancy.application.port.out.TenantProvisioningRepository;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class TenantProvisioningStateConventionTest {

  @Test
  void provisioningStateIsAnEnumAtThePortAndPersistenceBoundaries() throws NoSuchFieldException {
    assertThat(
            TenantProvisioningRepository.TenantProvisioningStatus.class.getRecordComponents()[0]
                .getType())
        .isEqualTo(com.emme.tenancy.domain.model.TenantProvisioningState.class);
    Field entityStatus = TenantRegistryEntity.class.getDeclaredField("status");
    assertThat(entityStatus.getType())
        .isEqualTo(com.emme.tenancy.domain.model.TenantProvisioningState.class);
  }

  @Test
  void tenantReadModelsUseTheAggregateStatusEnum() {
    assertThat(com.emme.tenancy.api.result.TenantDetails.class.getRecordComponents()[4].getType())
        .isEqualTo(TenantStatus.class);
    assertThat(
            com.emme.tenancy.adapter.in.web.response.TenantResponse.class.getRecordComponents()[3]
                .getType())
        .isEqualTo(TenantStatus.class);
    assertThat(
            com.emme.tenancy.api.result.TenantProvisioningStatus.class.getRecordComponents()[0]
                .getType())
        .isEqualTo(TenantProvisioningState.class);
  }
}
