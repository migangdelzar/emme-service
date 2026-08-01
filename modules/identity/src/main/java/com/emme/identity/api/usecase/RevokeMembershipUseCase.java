package com.emme.identity.api.usecase;

import com.emme.identity.api.command.RevokeMembershipCommand;
import com.emme.identity.api.result.MembershipInfo;

/** Revokes an existing Identity membership. */
public interface RevokeMembershipUseCase {

  MembershipInfo revoke(RevokeMembershipCommand command);
}
