package com.emme.buildlogic.secrets

import com.emme.buildlogic.environment.EnvironmentExtension
import com.emme.buildlogic.environment.EnvironmentName
import com.emme.buildlogic.environment.EnvironmentPlugin
import com.emme.buildlogic.secrets.model.SecretRotationMode
import com.emme.buildlogic.secrets.task.RotateSecretsTask
import com.emme.buildlogic.secrets.task.VerifySecretReferencesTask
import com.emme.buildlogic.secrets.task.VerifySecretsTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

/** Registers safe, provider-agnostic secret validation tasks. */
class SecretsPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.pluginManager.apply(EnvironmentPlugin::class.java)
    val environmentExtension = project.extensions.getByType(EnvironmentExtension::class.java)
    val extension = project.extensions.create<SecretsExtension>("secrets")
    extension.provider.convention(
      project.providers
        .gradleProperty("secret.provider")
        .orElse(environmentExtension.values.map { it["secrets.provider"].orEmpty() }.filter { it.isNotBlank() })
        .orElse(project.providers.environmentVariable("EMME_SECRET_PROVIDER"))
        .map(SecretProviderKind::parse)
        .orElse(SecretProviderKind.AUTO),
    )
    extension.manifest.convention(
      project.layout.projectDirectory.file("gradle/secrets/manifest.json"),
    )
    extension.rotationMode.convention(
      project.providers
        .gradleProperty("secret.rotation.mode")
        .orElse(environmentExtension.values.map { it["secrets.rotation.mode"].orEmpty() }.filter { it.isNotBlank() })
        .map(SecretRotationMode::parse)
        .orElse(SecretRotationMode.DRY_RUN),
    )

    project.tasks.register<VerifySecretsTask>("verifySecrets") {
      group = "security"
      description = "Verify required secret availability without exposing values"
      requiredNames.set(extension.required)
      provider.set(extension.provider)
    }
    project.tasks.register<VerifySecretReferencesTask>("verifySecretReferences") {
      group = "security"
      description = "Verify external secret reference declarations"
      references.set(extension.references)
    }
    project.tasks.register<RotateSecretsTask>("rotateSecrets") {
      group = "security"
      description = "Plan or explicitly apply provider-owned secret rotation"
      manifestPath.set(extension.manifest.map { it.asFile.absolutePath })
      environment.set(environmentExtension.name.map(EnvironmentName::id))
      provider.set(extension.provider)
      mode.set(extension.rotationMode)
      outputs.upToDateWhen { false }
    }
  }
}
