package com.emme.buildlogic.secrets.provider

import com.emme.buildlogic.secrets.SecretProviderKind
import com.emme.buildlogic.secrets.generator.SecretGenerator
import com.emme.buildlogic.secrets.model.SecretRotationMode
import com.emme.buildlogic.secrets.model.SecretRotationReport
import com.emme.buildlogic.secrets.model.SecretRotationRequest

/** Reads GitHub Environment secrets after Actions has injected them as variables. */
class GitHubActionsSecretProvider(
  private val environment: Map<String, String> = System.getenv(),
) : SecretProvider {
  private val delegate = EnvironmentSecretProvider(environment)

  override val kind = SecretProviderKind.GITHUB_ACTIONS

  override fun validate(requiredNames: Set<String>): Set<String> {
    check(!environment["GITHUB_ACTIONS"].isNullOrBlank()) {
      "GitHub Actions secret provider requires the GITHUB_ACTIONS environment"
    }
    return delegate.validate(requiredNames)
  }
}
