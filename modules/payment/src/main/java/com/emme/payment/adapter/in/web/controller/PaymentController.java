package com.emme.payment.adapter.in.web.controller;

import static com.emme.kernel.context.TenantContextHolder.withCurrentTenant;

import com.emme.payment.adapter.in.web.mapper.PaymentWebMapper;
import com.emme.payment.adapter.in.web.request.InitiatePaymentRequest;
import com.emme.payment.adapter.in.web.response.PaymentResponse;
import com.emme.payment.api.command.RefundPaymentCommand;
import com.emme.payment.api.query.GetPaymentQuery;
import com.emme.payment.api.query.ListPaymentsQuery;
import com.emme.payment.api.usecase.GetPaymentUseCase;
import com.emme.payment.api.usecase.InitiatePaymentUseCase;
import com.emme.payment.api.usecase.ListPaymentsUseCase;
import com.emme.payment.api.usecase.RefundPaymentUseCase;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
  private final InitiatePaymentUseCase initiatePayment;
  private final ListPaymentsUseCase listPayments;
  private final GetPaymentUseCase getPayment;
  private final RefundPaymentUseCase refundPayment;

  public PaymentController(
      InitiatePaymentUseCase initiatePayment,
      ListPaymentsUseCase listPayments,
      GetPaymentUseCase getPayment,
      RefundPaymentUseCase refundPayment) {
    this.initiatePayment = initiatePayment;
    this.listPayments = listPayments;
    this.getPayment = getPayment;
    this.refundPayment = refundPayment;
  }

  @GetMapping
  ResponseEntity<List<PaymentResponse>> list() {
    return withCurrentTenant(
        tenantId ->
            ResponseEntity.ok(
                listPayments.list(new ListPaymentsQuery(tenantId)).stream()
                    .map(PaymentResponse::from)
                    .toList()));
  }

  @PostMapping
  ResponseEntity<PaymentResponse> initiate(
      @Valid @RequestBody InitiatePaymentRequest request) {
    return withCurrentTenant(
        tenantId -> {
          var payment = initiatePayment.initiate(PaymentWebMapper.toCommand(tenantId, request));
          return ResponseEntity.created(URI.create("/api/payments/" + payment.id()))
              .body(PaymentResponse.from(payment));
        });
  }

  @GetMapping("/{id}")
  ResponseEntity<PaymentResponse> get(@PathVariable UUID id) {
    return withCurrentTenant(
        tenantId ->
            getPayment
                .get(new GetPaymentQuery(tenantId, id))
                .map(PaymentResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build()));
  }

  @PostMapping("/{id}/refund")
  ResponseEntity<PaymentResponse> refund(@PathVariable UUID id) {
    return withCurrentTenant(
        tenantId ->
            ResponseEntity.ok(
                PaymentResponse.from(
                    refundPayment.refund(new RefundPaymentCommand(tenantId, id)))));
  }
}
