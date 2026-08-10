package com.emme.identity.api.usecase;

import com.emme.identity.api.command.EnsureCustomerMembershipCommand;

/** Idempotently establishes a customer's membership for a tenant. */
public interface EnsureCustomerMembershipUseCase {

  void ensure(EnsureCustomerMembershipCommand command);
}
