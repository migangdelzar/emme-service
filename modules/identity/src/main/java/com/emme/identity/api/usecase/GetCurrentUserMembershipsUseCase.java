package com.emme.identity.api.usecase;

import com.emme.identity.api.query.GetCurrentUserMembershipsQuery;
import com.emme.identity.api.result.MembershipInfo;
import java.util.List;

/** Retrieves the active memberships visible for a user. */
public interface GetCurrentUserMembershipsUseCase {

  List<MembershipInfo> getMemberships(GetCurrentUserMembershipsQuery query);
}
