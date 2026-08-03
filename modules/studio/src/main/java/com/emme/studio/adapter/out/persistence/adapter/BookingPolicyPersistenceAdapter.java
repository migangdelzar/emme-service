package com.emme.studio.adapter.out.persistence.adapter;

import com.emme.studio.adapter.out.persistence.entity.BookingPolicyEntity;
import com.emme.studio.adapter.out.persistence.mapper.BookingPolicyPersistenceMapper;
import com.emme.studio.adapter.out.persistence.repository.SpringDataBookingPolicyRepository;
import com.emme.studio.application.port.out.BookingPolicyRepository;
import com.emme.studio.domain.model.BookingPolicy;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Implements the booking-policy persistence port using Spring Data JPA. */
@Component
public class BookingPolicyPersistenceAdapter implements BookingPolicyRepository {

  private final SpringDataBookingPolicyRepository repository;
  private final BookingPolicyPersistenceMapper mapper;

  public BookingPolicyPersistenceAdapter(SpringDataBookingPolicyRepository repository) {
    this.repository = repository;
    this.mapper = new BookingPolicyPersistenceMapper();
  }

  @Override
  public BookingPolicy save(BookingPolicy policy) {
    BookingPolicyEntity entity =
        policy.getId() == null
            ? mapper.toNewEntity(policy)
            : repository.findById(policy.getId()).orElseThrow();
    mapper.updateEntity(policy, entity);
    return mapper.toDomain(repository.save(entity));
  }

  @Override
  public Optional<BookingPolicy> findByTenantId(UUID tenantId) {
    return repository.findByTenantId(tenantId).map(mapper::toDomain);
  }
}
