package com.emme.payment.api.usecase;

import com.emme.payment.api.command.InitiatePaymentCommand;
import com.emme.payment.api.result.PaymentInfo;

public interface InitiatePaymentUseCase {
  PaymentInfo initiate(InitiatePaymentCommand command);
}
