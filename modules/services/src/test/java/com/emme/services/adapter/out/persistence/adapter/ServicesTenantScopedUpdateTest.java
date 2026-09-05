package com.emme.services.adapter.out.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.services.adapter.out.persistence.entity.ArtistEntity;
import com.emme.services.adapter.out.persistence.entity.ServiceEntity;
import com.emme.services.adapter.out.persistence.repository.SpringDataArtistRepository;
import com.emme.services.adapter.out.persistence.repository.SpringDataServiceRepository;
import com.emme.services.domain.model.Artist;
import com.emme.services.domain.model.ArtistStatus;
import com.emme.services.domain.model.Service;
import com.emme.services.domain.model.ServiceStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ServicesTenantScopedUpdateTest {

  @Test
  void listsArtistsFromTheCurrentTenantSchema() {
    SpringDataArtistRepository repository = org.mockito.Mockito.mock();
    ArtistPersistenceAdapter adapter = new ArtistPersistenceAdapter(repository);
    when(repository.findAll()).thenReturn(List.of(new ArtistEntity(UUID.randomUUID(), "Ada")));

    List<Artist> artists = adapter.findAll();

    verify(repository).findAll();
    assertThat(artists).hasSize(1);
  }

  @Test
  void listsServicesByStatusFromTheCurrentTenantSchema() {
    SpringDataServiceRepository repository = org.mockito.Mockito.mock();
    ServicePersistenceAdapter adapter = new ServicePersistenceAdapter(repository);
    when(repository.findByStatus(ServiceStatus.ACTIVE))
        .thenReturn(
            List.of(new ServiceEntity(UUID.randomUUID(), "CUT", "Cut", 30, BigDecimal.TEN)));

    List<Service> services = adapter.findByStatus(ServiceStatus.ACTIVE);

    verify(repository).findByStatus(ServiceStatus.ACTIVE);
    assertThat(services).hasSize(1);
  }

  @Test
  void updatesArtistByIdWithinTheTenantScopedConnection() {
    SpringDataArtistRepository repository = org.mockito.Mockito.mock();
    ArtistPersistenceAdapter adapter = new ArtistPersistenceAdapter(repository);
    UUID tenantId = UUID.randomUUID();
    UUID artistId = UUID.randomUUID();
    ArtistEntity entity = new ArtistEntity(tenantId, "Before");
    Artist artist = Artist.reconstitute(artistId, tenantId, "After", ArtistStatus.ACTIVE);
    when(repository.findById(artistId)).thenReturn(Optional.of(entity));
    when(repository.save(entity)).thenReturn(entity);

    Artist saved = adapter.save(artist);

    verify(repository).findById(artistId);
    assertThat(entity.getName()).isEqualTo("After");
    assertThat(saved.getTenantId()).isEqualTo(tenantId);
  }

  @Test
  void updatesServiceByIdWithinTheTenantScopedConnection() {
    SpringDataServiceRepository repository = org.mockito.Mockito.mock();
    ServicePersistenceAdapter adapter = new ServicePersistenceAdapter(repository);
    UUID tenantId = UUID.randomUUID();
    UUID serviceId = UUID.randomUUID();
    ServiceEntity entity = new ServiceEntity(tenantId, "CUT", "Before", 30, BigDecimal.TEN);
    Service service =
        Service.reconstitute(
            serviceId,
            tenantId,
            "CUT",
            "After",
            "Hair",
            null,
            45,
            BigDecimal.valueOf(25),
            ServiceStatus.ACTIVE);
    when(repository.findById(serviceId)).thenReturn(Optional.of(entity));
    when(repository.save(entity)).thenReturn(entity);

    Service saved = adapter.save(service);

    verify(repository).findById(serviceId);
    assertThat(entity.getName()).isEqualTo("After");
    assertThat(saved.getTenantId()).isEqualTo(tenantId);
  }
}
