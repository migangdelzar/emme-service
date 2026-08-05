package com.emme.services.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.services.application.port.out.ArtistCapabilityRepository;
import com.emme.services.domain.model.Artist;
import com.emme.services.domain.model.ArtistCapability;
import com.emme.services.domain.model.Service;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListArtistsForServiceServiceTest {

  @Test
  void listsActiveArtistsAssignedToTheRequestedService() {
    UUID tenantId = UUID.randomUUID();
    UUID serviceId = UUID.randomUUID();
    Artist artist = new Artist(tenantId, "Ada");
    Service service = new Service(tenantId, "cut", "Cut", 30, BigDecimal.TEN);
    ArtistCapability capability = new ArtistCapability(tenantId, artist, service);

    ListArtistsForServiceService subject =
        new ListArtistsForServiceService(
            new ArtistCapabilityRepository() {
              @Override
              public ArtistCapability save(ArtistCapability value) {
                return value;
              }

              @Override
              public Optional<ArtistCapability> findById(UUID id) {
                return Optional.empty();
              }

              @Override
              public List<ArtistCapability> findByArtistId(UUID id) {
                return List.of();
              }

              @Override
              public List<ArtistCapability> findByServiceIdAndActive(UUID id) {
                return id.equals(serviceId) ? List.of(capability) : List.of();
              }
            });

    assertThat(subject.list(tenantId, serviceId))
        .extracting("id", "name", "status")
        .containsExactly(org.assertj.core.groups.Tuple.tuple(artist.getId(), "Ada", "ACTIVE"));
  }
}
