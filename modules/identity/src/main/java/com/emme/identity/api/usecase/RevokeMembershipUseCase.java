package com.emme.identity.api.usecase;

import com.emme.identity.api.command.RevokeMembershipCommand;
import com.emme.identity.api.result.MembershipDetails;

/** Revokes an existing Identity membership. */
public interface RevokeMembershipUseCase {

  MembershipDetails revoke(RevokeMembershipCommand command);
}
