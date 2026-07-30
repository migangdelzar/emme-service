package com.emme.e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.client.E2eUserExtension;
import com.emme.client.Role;
import com.emme.client.UserSession;
import com.emme.client.WithUser;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(E2eUserExtension.class)
@WithUser(role = Role.BUSINESS_OWNER, tenant = "00000000-0000-0000-0000-100000000000")
class ServiceApiTest {

    private static final String DEMO = "00000000-0000-0000-0000-100000000000";
    private UserSession api;

    @BeforeEach
    void setUp(UserSession session) {
        this.api = session;
        session.setup().subscription(DEMO);
    }

    @Test
    void shouldListServices() {
        assertThat(api.services().list()).isNotNull().startsWith("[");
    }

    @Test
    void shouldCreateService() {
        String name = UUID.randomUUID().toString().substring(0, 8) + "-E2E-Svc";
        var result = api.services().create(name, "E2E-" + name.substring(0, 10), 500, 60, "Manicura");
        assertThat(result).isNotNull().contains("\"name\":\"" + name + "\"");
    }

    @Test
    void shouldFilterByCategory() {
        assertThat(api.services().listByCategory("Manicura")).isNotNull();
    }

    @Test
    void shouldRejectEmptyFields() {
        var result = api.post("/api/v1/services", "{\"name\":\"\",\"code\":\"\"}", 400);
        assertThat(result).isNotNull();
    }
}
