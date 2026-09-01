package com.emme.services.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.semantic.SemanticCacheDependencyChanged;
import com.emme.ai.contracts.semantic.SemanticCacheDependencyPublisher;
import com.emme.services.application.port.out.ServiceRepository;
import com.emme.services.domain.model.Service;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UpdateServiceCatalogEntryServiceTest {

  @Test
  void publishesTenantScopedQuoteTemplateInvalidationAtTheCatalogUpdateBoundary() {
    UUID tenantId = UUID.randomUUID();
    UUID serviceId = UUID.randomUUID();
    ServiceRepository repository = mock(ServiceRepository.class);
    SemanticCacheDependencyPublisher publisher = mock(SemanticCacheDependencyPublisher.class);
    Service service =
        Service.reconstitute(
            serviceId,
            tenantId,
            "gel",
            "Gel",
            "Nails",
            "Basic gel",
            60,
            BigDecimal.TEN,
            com.emme.services.domain.model.ServiceStatus.ACTIVE);
    when(repository.findById(serviceId)).thenReturn(Optional.of(service));
    when(repository.save(any())).thenReturn(service);

    new UpdateServiceCatalogEntryService(repository, publisher)
        .update(serviceId, "Gel Plus", "Nails", "Updated", 75, BigDecimal.valueOf(12));

    ArgumentCaptor<SemanticCacheDependencyChanged> events =
        ArgumentCaptor.forClass(SemanticCacheDependencyChanged.class);
    verify(publisher, org.mockito.Mockito.times(3)).publish(events.capture());
    assertThat(events.getAllValues())
        .anySatisfy(
            event -> {
              assertThat(event.dependency())
                  .isEqualTo(SemanticCacheDependencyChanged.Dependency.QUOTE_TEMPLATE);
              assertThat(event.tenantId()).isEqualTo(tenantId);
              assertThat(event.principalId()).isNull();
            });
  }
}
