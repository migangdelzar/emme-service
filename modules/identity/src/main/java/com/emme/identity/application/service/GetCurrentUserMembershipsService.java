package com.emme.identity.application.service;

import com.emme.identity.api.query.GetCurrentUserMembershipsQuery;
import com.emme.identity.api.result.MembershipDetails;
import com.emme.identity.api.usecase.GetCurrentUserMembershipsUseCase;
import com.emme.identity.application.mapper.MembershipApplicationMapper;
import com.emme.identity.application.port.out.MembershipRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Executes the GetCurrentUserMemberships use case. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetCurrentUserMembershipsService implements GetCurrentUserMembershipsUseCase {

  private final MembershipRepository membershipRepository;

  @Override
  public List<MembershipDetails> getMemberships(GetCurrentUserMembershipsQuery query) {
    return membershipRepository.findActiveByUserReference(query.userReference()).stream()
        .map(MembershipApplicationMapper::toResult)
        .toList();
  }
}
