package com.emme.studio.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ArtistTest {

  @Test
  void deactivatingArtistChangesItsBusinessStatus() {
    Artist artist = new Artist(UUID.randomUUID(), "Ada");

    artist.deactivate();

    assertThat(artist.getStatus()).isEqualTo(ArtistStatus.INACTIVE);
  }

  @Test
  void deactivatingCapabilityDoesNotChangeArtistOrService() {
    Artist artist = new Artist(UUID.randomUUID(), "Ada");
    Service service = new Service(UUID.randomUUID(), "cut", "Cut", 30, BigDecimal.TEN);
    ArtistCapability capability = new ArtistCapability(UUID.randomUUID(), artist, service);

    capability.deactivate();

    assertThat(capability.isActive()).isFalse();
    assertThat(capability.getArtist()).isSameAs(artist);
    assertThat(capability.getService()).isSameAs(service);
  }
}
