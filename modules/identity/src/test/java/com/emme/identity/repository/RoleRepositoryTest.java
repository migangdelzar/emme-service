package com.emme.identity.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.emme.identity.adapter.out.persistence.entity.Role;
import com.emme.identity.adapter.out.persistence.entity.RoleScope;
import com.emme.identity.adapter.out.persistence.repository.RoleRepository;
import com.emme.testing.BaseRepositoryTest;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** L2 repository tests for Role entity persistence and queries. */
class RoleRepositoryTest extends BaseRepositoryTest {

  @Autowired private RoleRepository roleRepo;

  @Test
  void shouldSaveAndFindRole() {
    Role role = roleRepo.save(new Role("admin", "Administrator", RoleScope.PLATFORM));

    Optional<Role> found = roleRepo.findById(role.getId());
    assertTrue(found.isPresent());
    assertEquals("admin", found.get().getCode());
    assertEquals("Administrator", found.get().getName());
    assertEquals(RoleScope.PLATFORM, found.get().getScope());
  }

  @Test
  void shouldFindByScope() {
    roleRepo.save(new Role("manager", "Manager", RoleScope.TENANT));
    roleRepo.save(new Role("superadmin", "Super Admin", RoleScope.PLATFORM));

    // Query all and filter by scope (RoleRepository has no findByScope method)
    long tenantRoles =
        roleRepo.findAll().stream().filter(r -> r.getScope() == RoleScope.TENANT).count();
    assertTrue(tenantRoles >= 1, "Should find at least one TENANT-scoped role");

    long platformRoles =
        roleRepo.findAll().stream().filter(r -> r.getScope() == RoleScope.PLATFORM).count();
    assertTrue(platformRoles >= 1, "Should find at least one PLATFORM-scoped role");
  }
}
