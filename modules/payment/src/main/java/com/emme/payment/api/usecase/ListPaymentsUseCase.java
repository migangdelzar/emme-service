package com.emme.payment.api.usecase;

import com.emme.payment.api.query.ListPaymentsQuery;
import com.emme.payment.api.result.PaymentDetails;
import java.util.List;

public interface ListPaymentsUseCase {
  List<PaymentDetails> list(ListPaymentsQuery query);
}
