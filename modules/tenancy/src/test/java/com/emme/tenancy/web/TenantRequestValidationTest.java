package com.emme.tenancy.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.tenancy.adapter.in.web.request.UpdateTenantRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TenantRequestValidationTest {

  private static jakarta.validation.ValidatorFactory validatorFactory;
  private static Validator validator;

  @BeforeAll
  static void setUpValidator() {
    validatorFactory = Validation.buildDefaultValidatorFactory();
    validator = validatorFactory.getValidator();
  }

  @AfterAll
  static void closeValidator() {
    validatorFactory.close();
  }

  @Test
  void rejectsTenantNameLongerThanPersistenceContract() {
    assertThat(validator.validate(new UpdateTenantRequest("n".repeat(151)))).isNotEmpty();
  }
}
