package com.emme.identity.application.service;

import com.emme.identity.api.query.GetEffectiveFeatureFlagsQuery;
import com.emme.identity.api.result.EffectiveFeatureFlags;
import com.emme.identity.api.usecase.GetEffectiveFeatureFlagsUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Executes the GetEffectiveFeatureFlags use case. */
@Service
@Transactional(readOnly = true)
public class GetEffectiveFeatureFlagsService implements GetEffectiveFeatureFlagsUseCase {

  private final FeatureFlagEvaluationService evaluationService;

  public GetEffectiveFeatureFlagsService(FeatureFlagEvaluationService evaluationService) {
    this.evaluationService = evaluationService;
  }

  @Override
  public EffectiveFeatureFlags get(GetEffectiveFeatureFlagsQuery query) {
    return new EffectiveFeatureFlags(evaluationService.getEffective(query.tenantId()));
  }
}
