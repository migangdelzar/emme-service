package com.emme.salon.adapter.out.persistence.adapter;

import com.emme.salon.adapter.out.persistence.entity.BookingPolicyEntity;
import com.emme.salon.adapter.out.persistence.mapper.BookingPolicyPersistenceMapper;
import com.emme.salon.adapter.out.persistence.repository.SpringDataBookingPolicyRepository;
import com.emme.salon.application.port.out.BookingPolicyRepository;
import com.emme.salon.domain.model.BookingPolicy;
import java.util.Optional;
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
  public Optional<BookingPolicy> find() {
    return repository.findFirstByOrderByCreatedAtAsc().map(mapper::toDomain);
  }
}
