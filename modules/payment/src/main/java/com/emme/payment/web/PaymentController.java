package com.emme.payment.web;

import static com.emme.kernel.context.TenantContextHolder.withCurrentTenant;

import com.emme.payment.application.PaymentService;
import com.emme.payment.entity.Payment;
import java.math.BigDecimal;
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
@RequestMapping("/api/v1/payments")
public class PaymentController {

  private final PaymentService service;

  public PaymentController(PaymentService service) {
    this.service = service;
  }

  record InitiateRequest(String providerReference, BigDecimal amount, String currency) {}

  record PaymentResponse(
      UUID id, String providerReference, BigDecimal amount, String currency, String status) {
    static PaymentResponse from(Payment p) {
      return new PaymentResponse(
          p.getId(),
          p.getProviderReference(),
          p.getAmount(),
          p.getCurrency(),
          p.getStatus().name());
    }
  }

  @GetMapping
  ResponseEntity<List<PaymentResponse>> list() {
    return withCurrentTenant(
        tenantId -> {
          var payments =
              service.findByTenantId(tenantId).stream().map(PaymentResponse::from).toList();
          return ResponseEntity.ok(payments);
        });
  }

  @PostMapping
  ResponseEntity<PaymentResponse> initiate(@RequestBody InitiateRequest req) {
    return withCurrentTenant(
        tenantId -> {
          var payment =
              service.initiate(tenantId, req.providerReference(), req.amount(), req.currency());
          var response = PaymentResponse.from(payment);
          var location = URI.create("/api/v1/payments/" + payment.getId());
          return ResponseEntity.created(location).body(response);
        });
  }

  @GetMapping("/{id}")
  ResponseEntity<PaymentResponse> get(@PathVariable UUID id) {
    return service
        .findById(id)
        .map(p -> ResponseEntity.ok(PaymentResponse.from(p)))
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping("/{id}/refund")
  ResponseEntity<PaymentResponse> refund(@PathVariable UUID id) {
    var payment = service.refund(id);
    return ResponseEntity.ok(PaymentResponse.from(payment));
  }
}
