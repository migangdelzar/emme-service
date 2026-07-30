package com.emme.e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;
import com.emme.client.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(E2eUserExtension.class)
@WithUser(role = Role.PLATFORM_ADMIN)
class NotificationApiTest {
    @Test void shouldGetNotifications(UserSession api) {
        var result = api.get("/api/v1/notifications", 200);
        assertThat(result).isNotNull();
    }
}
