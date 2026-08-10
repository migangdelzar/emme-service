package com.emme.identity.api.usecase;

import com.emme.identity.api.query.GetCurrentUserMembershipsQuery;
import com.emme.identity.api.result.MembershipDetails;
import java.util.List;

/** Retrieves the active memberships visible for a user. */
public interface GetCurrentUserMembershipsUseCase {

  List<MembershipDetails> getMemberships(GetCurrentUserMembershipsQuery query);
}
