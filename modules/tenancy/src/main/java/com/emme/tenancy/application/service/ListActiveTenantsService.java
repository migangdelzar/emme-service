package com.emme.tenancy.application.service;

import com.emme.tenancy.api.query.ListActiveTenantsQuery;
import com.emme.tenancy.api.result.TenantDetails;
import com.emme.tenancy.api.usecase.ListActiveTenantsUseCase;
import com.emme.tenancy.application.mapper.TenantApplicationMapper;
import com.emme.tenancy.application.port.out.TenantRepository;
import com.emme.tenancy.domain.model.TenantStatus;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ListActiveTenantsService implements ListActiveTenantsUseCase {
  private final TenantRepository repository;

  @Override
  public List<TenantDetails> list(ListActiveTenantsQuery query) {
    return repository.findByStatus(TenantStatus.ACTIVE).stream()
        .map(TenantApplicationMapper::toResult)
        .toList();
  }
}
