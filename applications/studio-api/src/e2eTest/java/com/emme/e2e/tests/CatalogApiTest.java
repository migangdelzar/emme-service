package com.emme.e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;
import com.emme.client.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(E2eUserExtension.class)
@WithUser(role = Role.PLATFORM_ADMIN)
class CatalogApiTest {
    @Test void shouldCreateCatalogItem(UserSession api) {
        var body = "{\"serviceId\":\"" + UUID.randomUUID() + "\",\"code\":\"E2E-CAT\",\"name\":\"E2E Catalog\"}";
        var result = api.post("/api/v1/catalog/items", body, 400);
        assertThat(result).isNotNull();
    }
    @Test void shouldMatchCatalog(UserSession api) {
        var result = api.post("/api/v1/catalog/match", "{\"query\":\"nails\"}", 500);
        assertThat(result).isNotNull();
    }
}
