package com.emme.payment.api.usecase;

import com.emme.payment.api.query.ListPaymentsQuery;
import com.emme.payment.api.result.PaymentInfo;
import java.util.List;

public interface ListPaymentsUseCase {
  List<PaymentInfo> list(ListPaymentsQuery query);
}
