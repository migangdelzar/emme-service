package com.emme.buildlogic.secrets.task

import com.emme.buildlogic.environment.EnvironmentName
import com.emme.buildlogic.secrets.SecretProviderKind
import com.emme.buildlogic.secrets.generator.SecureSecretGenerator
import com.emme.buildlogic.secrets.manifest.SecretManifestLoader
import com.emme.buildlogic.secrets.model.SecretRotationMode
import com.emme.buildlogic.secrets.model.SecretRotationReport
import com.emme.buildlogic.secrets.provider.SecretProviderFactory
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

/** Rotates provider-owned secrets from an environment manifest without exposing values. */
@DisableCachingByDefault(because = "Secret values must never enter Gradle cache state")
abstract class RotateSecretsTask : DefaultTask() {
  @get:Input
  abstract val manifestPath: Property<String>

  @get:Input
  abstract val environment: Property<String>

  @get:Input
  abstract val provider: Property<SecretProviderKind>

  @get:Input
  abstract val mode: Property<SecretRotationMode>

  @TaskAction
  fun rotate() {
    val requests = SecretManifestLoader().load(File(manifestPath.get()), environment.get())
    if (requests.isEmpty()) {
      logger.lifecycle("No secret rotation declarations for environment '{}'", environment.get())
      return
    }
    val selected = SecretProviderFactory.create(provider.get(), System.getenv())
    val report = selected.rotate(requests, mode.get(), SecureSecretGenerator())
    val unsupported = report.entries.filter { it.status == SecretRotationReport.Status.UNSUPPORTED }
    require(unsupported.isEmpty()) {
      "Secret provider '${report.provider}' does not support apply rotation for ${unsupported.size} entries"
    }
    logger.lifecycle(
      "Secret rotation {} for environment '{}' via {} ({} entries)",
      report.mode.id,
      environment.get(),
      report.provider,
      report.entries.size,
    )
  }
}
