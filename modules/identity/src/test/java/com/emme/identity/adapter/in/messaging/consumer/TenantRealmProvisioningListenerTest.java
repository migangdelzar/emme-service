package com.emme.identity.adapter.in.messaging.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.emme.identity.application.port.out.IdentityProviderAdministrationPort;
import com.emme.identity.application.port.out.IdentityRealmProvisioningConfigurationPort;
import com.emme.identity.application.port.out.IdentityRealmProvisioningSettings;
import com.emme.identity.application.port.out.TenantIdentityRealmPort;
import com.emme.tenancy.api.event.TenantRealmReady;
import com.emme.tenancy.api.event.TenantSchemaReady;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class TenantRealmProvisioningListenerTest {

  @Mock private IdentityProviderAdministrationPort administrationPort;
  @Mock private TenantIdentityRealmPort tenantIdentityRealmPort;
  @Mock private ApplicationEventPublisher eventPublisher;

  @Test
  void onTenantSchemaReady_createsRealmAndPublishesReady() throws Exception {
    UUID tenantId = UUID.randomUUID();
    TenantSchemaReady event =
        new TenantSchemaReady(UUID.randomUUID(), tenantId, "test-slug", "test_slug");
    IdentityRealmProvisioningSettings settings = provisioningSettings();
    IdentityRealmProvisioningConfigurationPort configuration = () -> settings;

    TenantRealmProvisioningListener listener =
        new TenantRealmProvisioningListener(
            administrationPort, tenantIdentityRealmPort, configuration, eventPublisher);

    listener.onTenantSchemaReady(event);

    verify(administrationPort).createRealm("emme-test-slug", "test-slug");
    verify(administrationPort)
        .createClient("emme-test-slug", settings.clientId(), settings.redirectUris());
    verify(administrationPort).createRealmRole("emme-test-slug", "business_owner");
    verify(administrationPort).createRealmRole("emme-test-slug", "nail_artist");
    verify(administrationPort)
        .createUser(
            "emme-test-slug",
            settings.initialAdminUsername(),
            "admin@test-slug.local",
            settings.initialAdminPassword(),
            settings.initialAdminRole());
    verify(tenantIdentityRealmPort).updateRealm(tenantId, "emme-test-slug");

    ArgumentCaptor<TenantRealmReady> captor = ArgumentCaptor.forClass(TenantRealmReady.class);
    verify(eventPublisher).publishEvent(captor.capture());
    assertThat(captor.getValue().keycloakRealm()).isEqualTo("emme-test-slug");
    assertThat(captor.getValue().tenantId()).isEqualTo(tenantId);
    assertThat(captor.getValue().slug()).isEqualTo("test-slug");
  }

  private static IdentityRealmProvisioningSettings provisioningSettings() {
    return new IdentityRealmProvisioningSettings(
        "emme-salon-app",
        List.of("http://localhost:8080/*"),
        "admin",
        "test-password",
        "business_owner",
        List.of("business_owner", "nail_artist"),
        3,
        2000);
  }
}
