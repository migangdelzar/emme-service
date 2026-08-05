package com.emme.buildlogic.environment

import com.emme.buildlogic.deployment.DeploymentTarget
import com.emme.buildlogic.environment.task.EnvironmentReportTask
import com.emme.buildlogic.environment.task.VerifyEnvironmentTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

/** Resolves environment configuration and exposes it to CDD build capabilities. */
class EnvironmentPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    val extension = project.extensions.create<EnvironmentExtension>("emmeEnvironment")
    project.configureEnvironmentExtension(extension)
    project.registerEnvironmentTasks(extension)
  }

  private fun Project.configureEnvironmentExtension(extension: EnvironmentExtension) {
    extension.environmentsDirectory.convention(
      rootProject.layout.projectDirectory
        .dir("gradle/environments"),
    )

    val preResolved = preResolvedEnvironmentContext()
    extension.name.convention(
      preResolved?.map(EnvironmentContext::name) ?: resolveEnvironmentName(),
    )
    val resolvedValues =
      preResolved?.map(EnvironmentContext::values) ?: run {
        val processValues = environmentProcessValues()
        val fileValues = environmentFileValues(extension)
        val baseValues =
          fileValues
            .zip(processValues) { file, process -> file + process }
        baseValues.zip(gradleValues()) { values, gradle -> values + gradle }
      }
    extension.values.convention(resolvedValues)
    extension.target.convention(
      extension
        .value("deployment.target", "compose", "emme.deployment.target")
        .map(DeploymentTarget::fromString),
    )
    extension.runtime.convention(
      extension
        .value("deployment.runtime", "jvm", "emme.deployment.runtime")
        .map(RuntimeKind::parse),
    )
    extension.imageTag.convention(
      extension.value("image.tag", "dev"),
    )
    extension.imageRegistry.convention(
      extension.value("image.registry", "local"),
    )
    extension.healthUrl.convention(
      extension.value("health.url", "http://localhost:8081/actuator/health"),
    )
    extension.kustomizeOverlay.convention(
      extension.value("kustomize.overlay", "dev"),
    )
  }

  private fun Project.registerEnvironmentTasks(extension: EnvironmentExtension) {
    tasks.register<VerifyEnvironmentTask>("verifyEnvironment") {
      group = "environment"
      description = "Validate the resolved non-secret environment configuration"
      environmentName.set(extension.name.map(EnvironmentName::id))
      target.set(extension.target.map { it.name.lowercase() })
      runtime.set(extension.runtime.map(RuntimeKind::id))
      overlay.set(extension.kustomizeOverlay)
    }
    tasks.register<EnvironmentReportTask>("environmentReport") {
      group = "environment"
      description = "Report resolved non-secret environment metadata"
      environmentName.set(extension.name.map(EnvironmentName::id))
      target.set(extension.target.map { it.name.lowercase() })
      runtime.set(extension.runtime.map(RuntimeKind::id))
      imageTag.set(extension.imageTag)
      imageRegistry.set(extension.imageRegistry)
      healthUrl.set(extension.healthUrl)
    }
  }

  private fun Project.resolveEnvironmentName(): Provider<EnvironmentName> =
    providers
      .gradleProperty("environment")
      .orElse(providers.environmentVariable("EMME_ENV"))
      .map(EnvironmentName::parse)
      .orElse(EnvironmentName.DEV)

  private fun Project.environmentFileValues(extension: EnvironmentExtension): Provider<Map<String, String>> {
    val file =
      extension.name.flatMap { environment ->
        extension.environmentsDirectory.file("${environment.id}.properties")
      }
    return providers.of(EnvironmentPropertiesValueSource::class.java) {
      parameters.file.set(file)
    }
  }

  private fun Project.environmentProcessValues(): Provider<Map<String, String>> =
    providers.environmentVariablesPrefixedBy("EMME_").map { values ->
      values
        .filterKeys { it != "EMME_ENV" }
        .mapKeys { (key, _) -> key.removePrefix("EMME_").lowercase().replace('_', '.') }
        .mapValues { (_, value) -> value.trim() }
        .filterValues(String::isNotBlank)
        .filterKeys { !EnvironmentContext.isSecretLike(it) }
    }

  private fun Project.gradleValues(): Provider<Map<String, String>> {
    val repositoryValues =
      providers.of(EnvironmentPropertiesValueSource::class.java) {
        parameters.file.set(layout.projectDirectory.file("gradle.properties"))
      }
    val commandLineValues =
      providers.provider {
        gradle.startParameter.projectProperties
          .filterKeys { !EnvironmentContext.isSecretLike(it) }
          .mapValues { (_, value) -> value.trim() }
          .filterValues(String::isNotBlank)
      }
    return repositoryValues.zip(commandLineValues) { repository, commandLine ->
      repository + commandLine
    }
  }

  private fun Project.preResolvedEnvironmentContext(): Provider<EnvironmentContext>? =
    if (
      gradle.extensions.extraProperties.has(ENVIRONMENT_NAME_KEY) &&
      gradle.extensions.extraProperties.has(ENVIRONMENT_VALUES_KEY)
    ) {
      @Suppress("UNCHECKED_CAST")
      val values = gradle.extensions.extraProperties.get(ENVIRONMENT_VALUES_KEY) as Map<String, String>
      val name =
        gradle.extensions.extraProperties
          .get(ENVIRONMENT_NAME_KEY)
          .toString()
      providers.provider { EnvironmentContext(EnvironmentName.parse(name), values) }
    } else {
      null
    }

  private companion object {
    const val ENVIRONMENT_NAME_KEY = "com.emme.environment.name"
    const val ENVIRONMENT_VALUES_KEY = "com.emme.environment.values"
  }
}
