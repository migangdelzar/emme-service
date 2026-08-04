package com.emme.tenancy.application.service;

import com.emme.tenancy.api.command.CreateTenantCommand;
import com.emme.tenancy.api.event.TenantCreated;
import com.emme.tenancy.api.result.TenantDetails;
import com.emme.tenancy.api.usecase.CreateTenantUseCase;
import com.emme.tenancy.application.mapper.TenantApplicationMapper;
import com.emme.tenancy.application.port.out.TenantRepository;
import com.emme.tenancy.domain.model.Tenant;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateTenantService implements CreateTenantUseCase {
  private final TenantRepository repository;
  private final ApplicationEventPublisher eventPublisher;

  public CreateTenantService(
      TenantRepository repository, ApplicationEventPublisher eventPublisher) {
    this.repository = repository;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public TenantDetails create(CreateTenantCommand command) {
    if (repository.existsBySlug(command.slug())) {
      throw new IllegalArgumentException(
          "Tenant with slug '" + command.slug() + "' already exists");
    }
    Tenant saved = repository.save(new Tenant(command.slug(), command.name()));
    eventPublisher.publishEvent(
        new TenantCreated(
            UUID.randomUUID(),
            saved.id(),
            saved.slug(),
            saved.name(),
            "admin@" + saved.slug() + ".emme.app"));
    return TenantApplicationMapper.toResult(saved);
  }
}
