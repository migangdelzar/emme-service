package com.emme.tenancy.application.service;

import com.emme.tenancy.api.query.ListActiveTenantsQuery;
import com.emme.tenancy.api.result.TenantInfo;
import com.emme.tenancy.api.usecase.ListActiveTenantsUseCase;
import com.emme.tenancy.application.mapper.TenantApplicationMapper;
import com.emme.tenancy.application.port.out.TenantRepository;
import com.emme.tenancy.domain.model.TenantStatus;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ListActiveTenantsService implements ListActiveTenantsUseCase {
  private final TenantRepository repository;

  public ListActiveTenantsService(TenantRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<TenantInfo> list(ListActiveTenantsQuery query) {
    return repository.findByStatus(TenantStatus.ACTIVE).stream()
        .map(TenantApplicationMapper::toInfo)
        .toList();
  }
}
