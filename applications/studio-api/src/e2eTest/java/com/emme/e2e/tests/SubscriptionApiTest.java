package com.emme.e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;
import com.emme.client.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(E2eUserExtension.class)
@WithUser(role = Role.BUSINESS_OWNER, tenant = "00000000-0000-0000-0000-100000000000")
class SubscriptionApiTest {
    private static final String DEMO = "00000000-0000-0000-0000-100000000000";
    
    @BeforeEach void setUp(UserSession api) { api.setup().subscription(DEMO); }
    
    @Test void shouldGetSubscriptions(UserSession api) {
        var result = api.post("/api/v1/subscriptions", "{}", 400);
        assertThat(result).isNotNull();
    }
}
