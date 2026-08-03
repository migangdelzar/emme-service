package com.emme.payment.api.usecase;

import com.emme.payment.api.command.ProcessPaymentCallbackCommand;
import com.emme.payment.api.result.PaymentInfo;

public interface ProcessPaymentCallbackUseCase {
  PaymentInfo process(ProcessPaymentCallbackCommand command);
}
