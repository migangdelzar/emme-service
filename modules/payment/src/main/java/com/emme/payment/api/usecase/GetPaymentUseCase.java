package com.emme.payment.api.usecase;

import com.emme.payment.api.query.GetPaymentQuery;
import com.emme.payment.api.result.PaymentInfo;
import java.util.Optional;

public interface GetPaymentUseCase {
  Optional<PaymentInfo> get(GetPaymentQuery query);
}
