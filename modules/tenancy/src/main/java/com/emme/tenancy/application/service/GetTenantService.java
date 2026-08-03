package com.emme.tenancy.application.service;

import com.emme.tenancy.api.query.GetTenantQuery;
import com.emme.tenancy.api.result.TenantInfo;
import com.emme.tenancy.api.usecase.GetTenantUseCase;
import com.emme.tenancy.application.mapper.TenantApplicationMapper;
import com.emme.tenancy.application.port.out.TenantRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetTenantService implements GetTenantUseCase {
  private final TenantRepository repository;

  public GetTenantService(TenantRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<TenantInfo> get(GetTenantQuery query) {
    return repository.findById(query.tenantId()).map(TenantApplicationMapper::toInfo);
  }
}
