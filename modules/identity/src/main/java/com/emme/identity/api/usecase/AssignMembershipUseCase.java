package com.emme.identity.api.usecase;

import com.emme.identity.api.command.AssignMembershipCommand;
import com.emme.identity.api.result.MembershipDetails;

/** Assigns a role-backed membership through the Identity module. */
public interface AssignMembershipUseCase {

  MembershipDetails assign(AssignMembershipCommand command);
}
