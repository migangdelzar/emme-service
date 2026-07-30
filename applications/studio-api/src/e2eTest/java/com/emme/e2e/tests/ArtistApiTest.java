package com.emme.e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.client.E2eUserExtension;
import com.emme.client.Role;
import com.emme.client.UserSession;
import com.emme.client.WithUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(E2eUserExtension.class)
@WithUser(role = Role.PLATFORM_ADMIN)
class ArtistApiTest {

    @Test
    void shouldListArtists(UserSession api) {
        assertThat(api.artists().list()).isNotNull();
        assertThat(api.artists().list()).startsWith("[");
    }

    @Test
    void shouldCreateAndGetArtistById(UserSession api) {
        var result = api.artists().create("E2E Artist");
        assertThat(result).contains("\"name\":\"E2E Artist\"");
    }

    @Test
    void shouldReturnNotFoundForUnknownId(UserSession api) {
        api.get("/api/v1/artists/00000000-0000-0000-0000-000000000000", 404);
    }
}
