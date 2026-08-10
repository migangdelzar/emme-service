package com.emme.buildlogic.secrets.provider

import com.emme.buildlogic.secrets.SecretProviderKind
import com.emme.buildlogic.secrets.generator.SecretGenerator
import com.emme.buildlogic.secrets.model.SecretRotationMode
import com.emme.buildlogic.secrets.model.SecretRotationReport
import com.emme.buildlogic.secrets.model.SecretRotationRequest

/** Port for one secret source; the provider owns validation and persistence details. */
interface SecretProvider {
  val kind: SecretProviderKind

  fun validate(requiredNames: Set<String>): Set<String>

  fun rotate(
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
            name = request.name,
            status =
              if (mode == SecretRotationMode.DRY_RUN) {
                SecretRotationReport.Status.PLANNED
              } else {
                SecretRotationReport.Status.UNSUPPORTED
              },
          )
        },
    )
}
