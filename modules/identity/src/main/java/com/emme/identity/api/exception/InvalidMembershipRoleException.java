package com.emme.identity.api.exception;

/** Raised when a role cannot be assigned to the requested membership scope. */
public final class InvalidMembershipRoleException extends IllegalArgumentException {

  private static final long serialVersionUID = 1L;

  public InvalidMembershipRoleException(String scope) {
    super("Role with scope " + scope + " cannot be assigned to a tenant membership");
  }
}
