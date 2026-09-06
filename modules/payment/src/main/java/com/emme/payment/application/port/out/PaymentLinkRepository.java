package com.emme.payment.application.port.out;

import com.emme.ai.contracts.payment.PaymentLink;
import java.util.Optional;

/** Tenant-schema persistence boundary for durable payment links. */
public interface PaymentLinkRepository {

  Optional<PaymentLink> findByIdempotencyKey(String idempotencyKey);

  PaymentLink save(PaymentLink link, String idempotencyKey);
}
