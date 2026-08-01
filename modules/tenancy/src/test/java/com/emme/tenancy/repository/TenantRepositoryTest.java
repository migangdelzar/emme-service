package com.emme.tenancy.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.tenancy.adapter.out.persistence.entity.Tenant;
import com.emme.tenancy.adapter.out.persistence.entity.TenantStatus;
import com.emme.tenancy.adapter.out.persistence.repository.TenantRepository;
import com.emme.testing.BaseRepositoryTest;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * L2 repository tests for Tenant JPA persistence. Extends {@link BaseRepositoryTest} —
 * transactional, H2, no web layer.
 */
@DisplayName("Tenant Repository")
class TenantRepositoryTest extends BaseRepositoryTest {

  @Autowired private TenantRepository tenantRepository;

  @Test
  @DisplayName("Save and find tenant by ID — generates UUIDv7 and timestamps")
  void shouldSaveAndFindTenant() {
    Tenant tenant = new Tenant("repo-test-slug", "Repo Test Salon");

    Tenant saved = tenantRepository.saveAndFlush(tenant);

    // ID generated
    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getId().version()).isEqualTo(7);

    // Timestamps populated
    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getUpdatedAt()).isNotNull();
    assertThat(saved.getCreatedAt()).isEqualTo(saved.getUpdatedAt());

    // Status defaults to ACTIVE
    assertThat(saved.getStatus()).isEqualTo(TenantStatus.ACTIVE);

    // Verify we can find it back by ID
    Optional<Tenant> found = tenantRepository.findById(saved.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getSlug()).isEqualTo("repo-test-slug");
    assertThat(found.get().getName()).isEqualTo("Repo Test Salon");
  }

  @Test
  @DisplayName("Find by slug returns correct tenant, unknown slug returns empty")
  void shouldFindBySlug() {
    Tenant tenant = new Tenant("repo-slug-find", "Slug Find Salon");
    tenantRepository.saveAndFlush(tenant);

    // Find by existing slug
    Optional<Tenant> found = tenantRepository.findBySlug("repo-slug-find");
    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("Slug Find Salon");
    assertThat(found.get().getStatus()).isEqualTo(TenantStatus.ACTIVE);

    // Unknown slug returns empty
    Optional<Tenant> missing = tenantRepository.findBySlug("nonexistent-slug");
    assertThat(missing).isEmpty();
  }
}
