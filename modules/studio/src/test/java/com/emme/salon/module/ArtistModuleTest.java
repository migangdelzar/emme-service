package com.emme.studio.module;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emme.testing.BaseSpringModuleTest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class ArtistModuleTest extends BaseSpringModuleTest {

  private UUID serviceId;

  @BeforeEach
  void setUp() throws Exception {
    fullSetup();

    // Create a service needed for capability tests
    MvcResult result =
        mockMvc
            .perform(
                post("/api/services")
                    .with(tenantJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"code\":\"mani\",\"name\":\"Manicure\",\"durationMinutes\":45,\"basePrice\":30.00}"))
            .andExpect(status().isCreated())
            .andReturn();
    serviceId =
        UUID.fromString(
            result.getResponse().getContentAsString().replaceAll(".*\"id\":\"([^\"]+)\".*", "$1"));
  }

  @Test
  void shouldCreateArtist() throws Exception {
    mockMvc
        .perform(
            post("/api/artists")
                .with(tenantJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Alice Styles\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.name").value("Alice Styles"))
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }

  @Test
  void shouldListArtists() throws Exception {
    // Create one first
    mockMvc
        .perform(
            post("/api/artists")
                .with(tenantJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"List Artist\"}"))
        .andExpect(status().isCreated());

    mockMvc
        .perform(get("/api/artists").with(tenantJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").isNotEmpty());
  }

  @Test
  void shouldUpdateArtistProfile() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/artists")
                    .with(tenantJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Old Name\"}"))
            .andExpect(status().isCreated())
            .andReturn();

    UUID artistId =
        UUID.fromString(
            result.getResponse().getContentAsString().replaceAll(".*\"id\":\"([^\"]+)\".*", "$1"));

    mockMvc
        .perform(
            put("/api/artists/{id}", artistId)
                .with(tenantJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"New Name\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("New Name"));
  }

  @Test
  void shouldAddCapabilityToArtist() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/artists")
                    .with(tenantJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Capable Artist\"}"))
            .andExpect(status().isCreated())
            .andReturn();

    UUID artistId =
        UUID.fromString(
            result.getResponse().getContentAsString().replaceAll(".*\"id\":\"([^\"]+)\".*", "$1"));

    mockMvc
        .perform(
            post("/api/artists/{id}/capabilities", artistId)
                .with(tenantJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"serviceId\":\"" + serviceId + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.active").value(true));
  }
}
