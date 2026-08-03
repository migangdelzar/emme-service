package com.emme.studio.documents.module;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emme.testing.BaseSpringModuleTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class DocumentModuleTest extends BaseSpringModuleTest {

  @BeforeEach
  void setUp() {
    fullSetup();
  }

  @Test
  void shouldListDocuments() throws Exception {
    mockMvc.perform(get("/api/documents").with(tenantJwt())).andExpect(status().isOk());
  }

  @Test
  void shouldRejectWithoutJwt() throws Exception {
    mockMvc
        .perform(
            post("/api/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Unauthorized Doc\",\"sourceType\":\"PDF\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void shouldReturnBadRequestForEmptyUpload() throws Exception {
    mockMvc
        .perform(
            post("/api/documents")
                .with(tenantJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"sourceType\":\"\"}"))
        .andExpect(status().isBadRequest());
  }
}
