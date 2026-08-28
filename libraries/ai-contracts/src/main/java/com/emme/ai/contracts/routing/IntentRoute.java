package com.emme.ai.contracts.routing;

import java.util.Objects;

/** Evidence-bearing routing result that can explicitly abstain. */
public record IntentRoute(
    RouteRequest request,
    IntentMatch top1,
    IntentMatch top2,
    double margin,
    boolean requiredSlotsComplete,
    boolean authorized,
    boolean abstained,
    String abstainReason) {

  public IntentRoute {
    request = Objects.requireNonNull(request, "request must not be null");
    top1 = Objects.requireNonNull(top1, "top1 must not be null");
    if (top2 != null && top2.similarity() > top1.similarity()) {
      throw new IllegalArgumentException("top1 similarity must be highest");
    }
    if (!Double.isFinite(margin) || margin < 0) {
      throw new IllegalArgumentException("margin must be finite and non-negative");
    }
    if (abstained && (abstainReason == null || abstainReason.isBlank())) {
      throw new IllegalArgumentException("abstained routes require a reason");
    }
    if (!abstained && abstainReason != null && !abstainReason.isBlank()) {
      throw new IllegalArgumentException("accepted routes must not contain an abstain reason");
    }
  }

  public static IntentRoute abstained(
      RouteRequest request,
      IntentMatch top1,
      IntentMatch top2,
      double margin,
      boolean requiredSlotsComplete,
      String reason) {
    return new IntentRoute(request, top1, top2, margin, requiredSlotsComplete, false, true, reason);
  }

  public static IntentRoute accepted(
      RouteRequest request,
      IntentMatch top1,
      IntentMatch top2,
      double margin,
      boolean requiredSlotsComplete) {
    return new IntentRoute(request, top1, top2, margin, requiredSlotsComplete, true, false, null);
  }
}
