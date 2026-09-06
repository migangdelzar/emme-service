package com.emme.payment.adapter.out.persistence.repository;

import com.emme.payment.adapter.out.persistence.entity.PaymentLinkEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataPaymentLinkRepository extends JpaRepository<PaymentLinkEntity, UUID> {

  Optional<PaymentLinkEntity> findByIdempotencyKey(String idempotencyKey);
}
