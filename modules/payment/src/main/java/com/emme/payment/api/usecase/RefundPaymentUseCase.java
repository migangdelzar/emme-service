package com.emme.payment.api.usecase;

import com.emme.payment.api.command.RefundPaymentCommand;
import com.emme.payment.api.result.PaymentDetails;

public interface RefundPaymentUseCase {
  PaymentDetails refund(RefundPaymentCommand command);
}
