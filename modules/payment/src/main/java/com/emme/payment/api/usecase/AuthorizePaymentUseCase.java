package com.emme.payment.api.usecase;

import com.emme.payment.api.command.AuthorizePaymentCommand;
import com.emme.payment.api.result.PaymentInfo;

public interface AuthorizePaymentUseCase {
  PaymentInfo authorize(AuthorizePaymentCommand command);
}
