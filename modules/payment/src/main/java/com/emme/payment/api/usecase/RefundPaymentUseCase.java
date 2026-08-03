package com.emme.payment.api.usecase;

import com.emme.payment.api.command.RefundPaymentCommand;
import com.emme.payment.api.result.PaymentInfo;

public interface RefundPaymentUseCase {
  PaymentInfo refund(RefundPaymentCommand command);
}
