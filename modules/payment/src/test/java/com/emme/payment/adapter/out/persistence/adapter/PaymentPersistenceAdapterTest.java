package com.emme.payment.adapter.out.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.payment.adapter.out.persistence.mapper.PaymentPersistenceMapper;
import com.emme.payment.adapter.out.persistence.repository.SpringDataPaymentRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaymentPersistenceAdapterTest {

  @Test
  void listsPaymentsFromTheCurrentTenantSchema() {
    SpringDataPaymentRepository repository = org.mockito.Mockito.mock();
    PaymentPersistenceAdapter adapter =
        new PaymentPersistenceAdapter(repository, new PaymentPersistenceMapper());
    when(repository.findAll()).thenReturn(List.of());

    assertThat(adapter.findAll()).isEmpty();

    verify(repository).findAll();
  }
}
