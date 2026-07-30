package com.emme.e2e.tests;

import static com.emme.client.E2eTest.withUnauthenticated;
import static org.assertj.core.api.Assertions.assertThat;

import com.emme.client.E2eUserExtension;
import com.emme.client.Role;
import com.emme.client.UserSession;
import com.emme.client.WithUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(E2eUserExtension.class)
@WithUser(role = Role.PLATFORM_ADMIN)
class DocumentApiTest {

    @Test
    void shouldRejectUnauthenticated() {
        withUnauthenticated(s -> {
            var result = s.get("/api/v1/documents", 401);
            assertThat(result).isNotNull();
        });
    }

    @Test
    void shouldListWithAuth(UserSession api) {
        var result = api.get("/api/v1/documents", 200);
        assertThat(result).isNotNull();
    }
}
