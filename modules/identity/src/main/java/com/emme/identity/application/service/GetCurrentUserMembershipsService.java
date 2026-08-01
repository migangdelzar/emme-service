package com.emme.identity.application.service;

import com.emme.identity.api.query.GetCurrentUserMembershipsQuery;
import com.emme.identity.api.result.MembershipInfo;
import com.emme.identity.api.usecase.GetCurrentUserMembershipsUseCase;
import com.emme.identity.application.mapper.MembershipApplicationMapper;
import com.emme.identity.application.port.out.MembershipRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Executes the GetCurrentUserMemberships use case. */
@Service
@Transactional(readOnly = true)
public class GetCurrentUserMembershipsService implements GetCurrentUserMembershipsUseCase {

  private final MembershipRepository membershipRepository;

  public GetCurrentUserMembershipsService(MembershipRepository membershipRepository) {
    this.membershipRepository = membershipRepository;
  }

  @Override
  public List<MembershipInfo> getMemberships(GetCurrentUserMembershipsQuery query) {
    return membershipRepository.findActiveByUserReference(query.userReference()).stream()
        .map(MembershipApplicationMapper::toInfo)
        .toList();
  }
}
