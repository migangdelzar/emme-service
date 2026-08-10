package com.emme.buildlogic.secrets.provider

import com.emme.buildlogic.secrets.SecretProviderKind
import com.emme.buildlogic.secrets.generator.SecretGenerator
import com.emme.buildlogic.secrets.model.SecretRotationMode
import com.emme.buildlogic.secrets.model.SecretRotationReport
import com.emme.buildlogic.secrets.model.SecretRotationRequest

/** Validates secrets injected into the current process without logging values. */
class EnvironmentSecretProvider(
  private val environment: Map<String, String> = System.getenv(),
) : SecretProvider {
  override val kind = SecretProviderKind.ENVIRONMENT

  override fun validate(requiredNames: Set<String>): Set<String> =
    requiredNames.filterTo(linkedSetOf()) { name -> environment[name].isNullOrBlank() }

  override fun rotate(
    requests: List<SecretRotationRequest>,
    mode: SecretRotationMode,
    generator: SecretGenerator,
  ): SecretRotationReport =
    SecretRotationReport(
      provider = kind.id,
      mode = mode,
      entries =
        requests.map { request ->
          SecretRotationReport.Entry(
            request.name,
            if (mode == SecretRotationMode.DRY_RUN) {
              SecretRotationReport.Status.PLANNED
            } else {
              SecretRotationReport.Status.UNSUPPORTED
            },
          )
        },
    )
}
