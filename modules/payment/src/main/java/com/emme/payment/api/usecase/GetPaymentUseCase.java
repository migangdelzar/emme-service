package com.emme.payment.api.usecase;

import com.emme.payment.api.query.GetPaymentQuery;
import com.emme.payment.api.result.PaymentDetails;
import java.util.Optional;

public interface GetPaymentUseCase {
  Optional<PaymentDetails> get(GetPaymentQuery query);
}
