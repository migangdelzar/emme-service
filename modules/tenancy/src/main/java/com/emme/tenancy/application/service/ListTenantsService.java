package com.emme.tenancy.application.service;

import com.emme.tenancy.api.query.ListTenantsQuery;
import com.emme.tenancy.api.result.TenantInfo;
import com.emme.tenancy.api.usecase.ListTenantsUseCase;
import com.emme.tenancy.application.mapper.TenantApplicationMapper;
import com.emme.tenancy.application.port.out.TenantRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ListTenantsService implements ListTenantsUseCase {
  private final TenantRepository repository;

  public ListTenantsService(TenantRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<TenantInfo> list(ListTenantsQuery query) {
    return repository.findAll().stream().map(TenantApplicationMapper::toInfo).toList();
  }
}
