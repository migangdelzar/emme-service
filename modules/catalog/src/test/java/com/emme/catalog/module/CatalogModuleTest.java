package com.emme.catalog.module;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emme.tenancy.testing.BaseTenantModuleTest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class CatalogModuleTest extends BaseTenantModuleTest {

  @BeforeEach
  void setUp() {
    fullSetup();
  }

  @Test
  void shouldCreateCatalogItem() throws Exception {
    mockMvc
        .perform(
            post("/api/catalog/items")
                .with(tenantJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                    "serviceId": "%s",
                                    "code": "MANI-GEL-001",
                                    "name": "Gel Manicure",
                                    "description": "Premium gel manicure",
                                    "price": 35.00,
                                    "durationMinutes": 45
                                }
                                """
                        .formatted(UUID.randomUUID().toString())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.name").value("Gel Manicure"));
  }

  @Test
  void shouldListCatalogItems() throws Exception {
    mockMvc
        .perform(
            post("/api/catalog/items")
                .with(tenantJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                    "serviceId": "%s",
                                    "code": "PEDI-BSC-001",
                                    "name": "Basic Pedicure",
                                    "description": "Classic pedicure service",
                                    "price": 28.00,
                                    "durationMinutes": 40
                                }
                                """
                        .formatted(UUID.randomUUID().toString())))
        .andExpect(status().isCreated());

    mockMvc
        .perform(get("/api/catalog/items").with(tenantJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").isNotEmpty());
  }

  @Test
  void shouldDeleteCatalogItem() throws Exception {
    var result =
        mockMvc
            .perform(
                post("/api/catalog/items")
                    .with(tenantJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                                {
                                    "serviceId": "%s",
                                    "code": "NAIL-ART-001",
                                    "name": "Nail Art",
                                    "description": "Custom nail art design",
                                    "price": 15.00,
                                    "durationMinutes": 30
                                }
                                """
                            .formatted(UUID.randomUUID().toString())))
            .andExpect(status().isCreated())
            .andReturn();

    String location = result.getResponse().getHeader("Location");
    String itemId = location.substring(location.lastIndexOf("/") + 1);

    mockMvc
        .perform(delete("/api/catalog/items/{id}", itemId).with(tenantJwt()))
        .andExpect(status().isNoContent());
  }

  @Test
  void shouldRejectWithoutJwt() throws Exception {
    mockMvc
        .perform(
            post("/api/catalog/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                    "serviceId": "%s",
                                    "code": "TEST-001",
                                    "name": "Test Item",
                                    "price": 10.00
                                }
                                """
                        .formatted(UUID.randomUUID().toString())))
        .andExpect(status().isUnauthorized());
  }
}
