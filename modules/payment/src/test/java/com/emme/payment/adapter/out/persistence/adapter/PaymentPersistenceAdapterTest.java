package com.emme.payment.adapter.out.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.payment.adapter.out.persistence.entity.PaymentEntity;
import com.emme.payment.adapter.out.persistence.mapper.PaymentPersistenceMapper;
import com.emme.payment.adapter.out.persistence.repository.SpringDataPaymentRepository;
import com.emme.payment.domain.model.Payment;
import com.emme.payment.domain.model.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentPersistenceAdapterTest {

  @Test
  void listsPaymentsFromTheCurrentTenantSchema() {
    SpringDataPaymentRepository repository = mock();
    PaymentPersistenceAdapter adapter =
        new PaymentPersistenceAdapter(repository, new PaymentPersistenceMapper());
    when(repository.findAll()).thenReturn(List.of());

    assertThat(adapter.findAll()).isEmpty();

    verify(repository).findAll();
  }

  @Test
  void updatesTheManagedEntityWhenSavingAnExistingPayment() {
    SpringDataPaymentRepository repository = mock();
    PaymentPersistenceMapper mapper = new PaymentPersistenceMapper();
    PaymentPersistenceAdapter adapter = new PaymentPersistenceAdapter(repository, mapper);
    UUID paymentId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    Instant updatedAt = Instant.now();
    Payment payment =
        Payment.restore(
            paymentId,
            tenantId,
            "provider-1",
            BigDecimal.TEN,
            "MXN",
            PaymentStatus.PENDING,
            updatedAt);
    payment.authorize();
    PaymentEntity managedEntity = new PaymentEntity(tenantId, "provider-1", BigDecimal.TEN, "MXN");
    managedEntity.restoreIdentity(paymentId, updatedAt);
    when(repository.findById(paymentId)).thenReturn(Optional.of(managedEntity));
    when(repository.save(managedEntity)).thenReturn(managedEntity);

    Payment saved = adapter.save(payment);

    verify(repository).findById(paymentId);
    verify(repository).save(managedEntity);
    assertThat(saved.status()).isEqualTo(PaymentStatus.AUTHORIZED);
  }
}
