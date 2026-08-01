package com.emme.tenancy.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.tenancy.adapter.out.persistence.entity.TenantEntity;
import com.emme.tenancy.adapter.out.persistence.repository.SpringDataTenantRepository;
import com.emme.tenancy.domain.model.TenantStatus;
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

  @Autowired private SpringDataTenantRepository tenantRepository;

  @Test
  @DisplayName("Save and find tenant by ID — generates UUIDv7 and timestamps")
  void shouldSaveAndFindTenant() {
    TenantEntity tenant = new TenantEntity();
    tenant.setSlug("repo-test-slug");
    tenant.setName("Repo Test Salon");

    TenantEntity saved = tenantRepository.saveAndFlush(tenant);

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
    Optional<TenantEntity> found = tenantRepository.findById(saved.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getSlug()).isEqualTo("repo-test-slug");
    assertThat(found.get().getName()).isEqualTo("Repo Test Salon");
  }

  @Test
  @DisplayName("Find by slug returns correct tenant, unknown slug returns empty")
  void shouldFindBySlug() {
    TenantEntity tenant = new TenantEntity();
    tenant.setSlug("repo-slug-find");
    tenant.setName("Slug Find Salon");
    tenantRepository.saveAndFlush(tenant);

    // Find by existing slug
    Optional<TenantEntity> found = tenantRepository.findBySlug("repo-slug-find");
    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("Slug Find Salon");
    assertThat(found.get().getStatus()).isEqualTo(TenantStatus.ACTIVE);

    // Unknown slug returns empty
    Optional<TenantEntity> missing = tenantRepository.findBySlug("nonexistent-slug");
    assertThat(missing).isEmpty();
  }
}
