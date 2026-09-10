package com.emme.client;

import java.util.List;

/**
 * Immutable collection of sessions provisioned for one E2E test invocation.
 *
 * <p>Use this record when a scenario needs to exercise interactions between distinct identities:
 *
 * <pre>{@code
 * @WithUser(roles = {Roles.TENANT_OWNER})
 * @WithUser(roles = {Roles.TENANT_STAFF})
 * void ownerAndStaff(E2eUsers users) {
 *   users.first().tenants().list();
 *   users.get(1).customers().list();
 * }
 * }</pre>
 */
public record E2eUsers(List<UserSession> sessions) {

  public E2eUsers {
    sessions = List.copyOf(sessions);
    if (sessions.isEmpty()) {
      throw new IllegalArgumentException("At least one E2E user is required");
    }
  }

  /** Returns the first configured session. */
  public UserSession first() {
    return sessions.getFirst();
  }

  /** Returns the session at the zero-based configuration index. */
  public UserSession get(int index) {
    return sessions.get(index);
  }

  /** Number of configured users. */
  public int size() {
    return sessions.size();
  }
}
