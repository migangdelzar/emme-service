package com.emme.catalog.adapter.in.web.advice;

import com.emme.catalog.api.exception.CatalogItemNotFoundException;
import com.emme.kernel.tracing.CorrelationId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CatalogExceptionHandler {

  @ExceptionHandler(CatalogItemNotFoundException.class)
  ProblemDetail handleNotFound(CatalogItemNotFoundException ex) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    pd.setProperty("requestId", CorrelationId.get());
    return pd;
  }
}
