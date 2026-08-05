package com.emme.buildlogic.secrets.provider

import com.emme.buildlogic.secrets.SecretProviderKind

/** One provider-selection policy shared by verification and rotation tasks. */
object SecretProviderFactory {
  fun create(
    requested: SecretProviderKind,
    environment: Map<String, String>,
  ): SecretProvider =
    when (val selected = select(requested, environment)) {
      SecretProviderKind.ENVIRONMENT -> EnvironmentSecretProvider(environment)
      SecretProviderKind.BITWARDEN -> BitwardenSecretProvider(environment)
      SecretProviderKind.GITHUB_ACTIONS -> GitHubActionsSecretProvider(environment)
      SecretProviderKind.KUBERNETES -> KubernetesSecretReferenceProvider()
      SecretProviderKind.AUTO -> error("Provider selection must resolve before creation")
    }

  fun select(
    requested: SecretProviderKind,
    environment: Map<String, String>,
  ): SecretProviderKind =
    when {
      requested != SecretProviderKind.AUTO -> {
        requested
      }

      !environment["GITHUB_ACTIONS"].isNullOrBlank() -> {
        SecretProviderKind.GITHUB_ACTIONS
      }

      !environment["BW_SESSION"].isNullOrBlank() || !environment["BWS_ACCESS_TOKEN"].isNullOrBlank() -> {
        SecretProviderKind.BITWARDEN
      }

      else -> {
        SecretProviderKind.ENVIRONMENT
      }
    }
}
