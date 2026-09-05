package com.emme.identity.module;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emme.identity.adapter.out.persistence.entity.MembershipEntity;
import com.emme.identity.adapter.out.persistence.entity.PermissionEntity;
import com.emme.identity.adapter.out.persistence.entity.RoleEntity;
import com.emme.identity.adapter.out.persistence.entity.RolePermissionEntity;
import com.emme.identity.adapter.out.persistence.repository.PermissionRepository;
import com.emme.identity.adapter.out.persistence.repository.RolePermissionRepository;
import com.emme.identity.adapter.out.persistence.repository.SpringDataMembershipRepository;
import com.emme.identity.adapter.out.persistence.repository.SpringDataRoleRepository;
import com.emme.identity.domain.model.MembershipStatus;
import com.emme.identity.domain.model.RoleScope;
import com.emme.tenancy.testing.BaseTenantModuleTest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** L3 module tests for identity endpoints (memberships CRUD, current user, permissions). */
class IdentityModuleTest extends BaseTenantModuleTest {

  private static final String PERMISSIONS_TEST_USER = "auth0|permissions-test-user";

  @Autowired private SpringDataMembershipRepository membershipRepo;
  @Autowired private SpringDataRoleRepository roleRepo;
  @Autowired private PermissionRepository permissionRepo;
  @Autowired private RolePermissionRepository rolePermissionRepo;

  private UUID tenantId;
  private RoleEntity savedRole;

  @BeforeEach
  void setUp() {
    tenantId = fullSetup();
    savedRole =
        roleRepo.save(
            new RoleEntity("test-role-" + System.nanoTime(), "Test Role", RoleScope.TENANT));
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
            post("/api/identity/memberships")
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
  void shouldRejectMembershipAssignmentForAStaffUser() throws Exception {
    String requestBody =
        """
                {
                    "tenantId": "%s",
                    "roleId": "%s",
                    "userReference": "staff-assignment-target"
                }
                """
            .formatted(tenantId, savedRole.getId());

    mockMvc
        .perform(
            post("/api/identity/memberships")
                .with(tenantJwt(tenantId, TEST_USER_SUB, "staff"))
                .contentType("application/json")
                .content(requestBody))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldRejectTenantOwnerAssignmentIntoAnotherTenant() throws Exception {
    UUID otherTenantId = createTenant("other-" + System.nanoTime(), "Other Salon").id();
    String requestBody =
        """
                {
                    "tenantId": "%s",
                    "roleId": "%s",
                    "userReference": "cross-tenant-target"
                }
                """
            .formatted(otherTenantId, savedRole.getId());

    mockMvc
        .perform(
            post("/api/identity/memberships")
                .with(tenantJwt(tenantId, TEST_USER_SUB, "tenant_owner"))
                .contentType("application/json")
                .content(requestBody))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldRevokeMembership() throws Exception {
    // Create membership via repository (role loaded eagerly via constructor)
    MembershipEntity m =
        membershipRepo.save(new MembershipEntity(tenantId, savedRole, "revoke-test-user"));

    mockMvc
        .perform(delete("/api/identity/memberships/{id}", m.getId()).with(tenantJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REVOKED"));

    // Verify DB state
    MembershipEntity revoked = membershipRepo.findById(m.getId()).orElseThrow();
    assertEquals(MembershipStatus.REVOKED, revoked.getStatus());
  }

  @Test
  void shouldGetCurrentUserMemberships() throws Exception {
    // Create a membership so the user has something
    membershipRepo.save(new MembershipEntity(tenantId, savedRole, TEST_USER_SUB));

    mockMvc
        .perform(get("/api/identity/me").with(tenantJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].role").value(savedRole.getCode()));
  }

  @Test
  void shouldGetCurrentUserPermissions() throws Exception {
    PermissionEntity permission =
        permissionRepo.save(new PermissionEntity("quotes.read", "Read quotes", "Read quote data"));
    rolePermissionRepo.save(new RolePermissionEntity(savedRole, permission));
    membershipRepo.save(new MembershipEntity(tenantId, savedRole, PERMISSIONS_TEST_USER));

    mockMvc
        .perform(
            get("/api/identity/me/permissions")
                .with(tenantJwt(tenantId, PERMISSIONS_TEST_USER, "tenant_owner"))
                .param("tenantId", tenantId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(hasItem("quotes.read")));
  }

  @Test
  void shouldRejectUnauthenticatedMembershipAccess() throws Exception {
    mockMvc.perform(get("/api/identity/me")).andExpect(status().is4xxClientError());
  }
}
