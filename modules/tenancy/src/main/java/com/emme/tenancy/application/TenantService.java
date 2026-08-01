package com.emme.tenancy.application;

import com.emme.tenancy.api.event.TenantCreated;
import com.emme.tenancy.application.port.out.TenantRepository;
import com.emme.tenancy.domain.model.Tenant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TenantService {

  private final TenantRepository repository;
  private final ApplicationEventPublisher eventPublisher;

  public TenantService(TenantRepository repository, ApplicationEventPublisher eventPublisher) {
    this.repository = repository;
    this.eventPublisher = eventPublisher;
  }

  public Tenant create(String slug, String name) {
    if (repository.existsBySlug(slug)) {
      throw new IllegalArgumentException("Tenant with slug '" + slug + "' already exists");
    }
    Tenant saved = repository.save(new Tenant(slug, name));
    eventPublisher.publishEvent(
        new TenantCreated(
            saved.id(), saved.slug(), saved.name(), "admin@" + saved.slug() + ".emme.app"));
    return saved;
  }

  public Tenant update(UUID id, String name) {
    Tenant tenant =
        repository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + id));
    tenant.rename(name);
    return repository.save(tenant);
  }

  public Tenant suspend(UUID id) {
    Tenant tenant =
        repository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + id));
    tenant.suspend();
    return repository.save(tenant);
  }

  public Tenant reactivate(UUID id) {
    Tenant tenant =
        repository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + id));
    tenant.reactivate();
    return repository.save(tenant);
  }

  public Tenant stageDelete(UUID id) {
    Tenant tenant =
        repository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + id));
    tenant.markDeleted();
    return repository.save(tenant);
  }

  @Transactional(readOnly = true)
  public Optional<Tenant> findById(UUID id) {
    return repository.findById(id);
  }

  @Transactional(readOnly = true)
  public Optional<Tenant> findBySlug(String slug) {
    return repository.findBySlug(slug);
  }

  @Transactional(readOnly = true)
  public List<Tenant> findAll() {
    return repository.findAll();
  }
}
