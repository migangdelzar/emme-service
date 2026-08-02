package com.emme.studio.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emme.testing.BaseWebTest;
import org.junit.jupiter.api.Test;

class DashboardWebTest extends BaseWebTest {

  @Test
  void shouldReturnSseStream() throws Exception {
    mockMvc.perform(get("/api/dashboard/stream").with(auth())).andExpect(status().isOk());
  }

  @Test
  void shouldRejectWithoutJwt() throws Exception {
    mockMvc.perform(get("/api/dashboard/stream")).andExpect(status().isUnauthorized());
  }
}
