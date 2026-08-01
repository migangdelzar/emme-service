package com.emme.identity.module;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emme.identity.adapter.out.persistence.entity.Membership;
import com.emme.identity.adapter.out.persistence.entity.MembershipStatus;
import com.emme.identity.adapter.out.persistence.entity.Role;
import com.emme.identity.adapter.out.persistence.entity.RoleScope;
import com.emme.identity.adapter.out.persistence.repository.MembershipRepository;
import com.emme.testing.BaseSpringModuleTest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** L3 module tests for identity endpoints (memberships CRUD, current user, permissions). */
class IdentityModuleTest extends BaseSpringModuleTest {

  @Autowired private MembershipRepository membershipRepo;

  private UUID tenantId;
  private Role savedRole;

  @BeforeEach
  void setUp() {
    tenantId = fullSetup();
    savedRole =
        roleRepo.save(new Role("test-role-" + System.nanoTime(), "Test Role", RoleScope.TENANT));
  }

  @Test
  void shouldAssignMembership() throws Exception {
    String requestBody =
        """
                {
                    "tenantId": "%s",
                    "roleId": "%s",
                    "userReference": "test-user-ref"
                }
                """
            .formatted(tenantId, savedRole.getId());

    mockMvc
        .perform(
            post("/api/v1/identity/memberships")
                .with(tenantJwt())
                .contentType("application/json")
                .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.tenantId").value(tenantId.toString()))
        .andExpect(jsonPath("$.role").value(savedRole.getCode()))
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }

  @Test
  void shouldRevokeMembership() throws Exception {
    // Create membership via repository (role loaded eagerly via constructor)
    Membership m = membershipRepo.save(new Membership(tenantId, savedRole, "revoke-test-user"));

    mockMvc
        .perform(delete("/api/v1/identity/memberships/{id}", m.getId()).with(tenantJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REVOKED"));

    // Verify DB state
    Membership revoked = membershipRepo.findById(m.getId()).orElseThrow();
    assertEquals(MembershipStatus.REVOKED, revoked.getStatus());
  }

  @Test
  void shouldGetCurrentUserMemberships() throws Exception {
    // Create a membership so the user has something
    membershipRepo.save(new Membership(tenantId, savedRole, TEST_USER_SUB));

    mockMvc
        .perform(get("/api/v1/identity/me").with(tenantJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].role").value(savedRole.getCode()));
  }

  @Test
  void shouldGetCurrentUserPermissions() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/identity/me/permissions")
                .with(tenantJwt())
                .param("tenantId", tenantId.toString()))
        .andExpect(status().isOk());
  }

  @Test
  void shouldRejectUnauthenticatedMembershipAccess() throws Exception {
    mockMvc.perform(get("/api/v1/identity/me")).andExpect(status().is4xxClientError());
  }
}
