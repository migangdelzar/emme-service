package com.emme.buildlogic.settings

import java.io.File
import java.util.Properties
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

/** Resolves non-secret environment properties before project plugin resolution. */
class EnvironmentSettingsPlugin : Plugin<Settings> {
  override fun apply(settings: Settings) {
    val environment = resolveEnvironment(settings)
    val values = resolveValues(settings, environment)
    settings.gradle.extensions.extraProperties.set(ENVIRONMENT_NAME_KEY, environment.id)
    settings.gradle.extensions.extraProperties.set(ENVIRONMENT_VALUES_KEY, values)
  }

  private fun resolveEnvironment(settings: Settings): EnvironmentName =
    settings.providers
      .gradleProperty("environment")
      .orElse(settings.providers.environmentVariable("EMME_ENV"))
      .map(EnvironmentName::parse)
      .getOrElse(EnvironmentName.DEV)

  private fun resolveValues(
    settings: Settings,
    environment: EnvironmentName,
  ): Map<String, String> {
    val fileValues = loadProperties(settings.rootDir.resolve("gradle/environments/${environment.id}.properties"))
    val processValues =
      System.getenv()
        .filterKeys { it.startsWith("EMME_") && it != "EMME_ENV" }
        .mapKeys { (key, _) -> key.removePrefix("EMME_").lowercase().replace('_', '.') }
        .filterKeys { !isSecretLike(it) }
    val repositoryValues =
      loadProperties(settings.rootDir.resolve("gradle.properties"))
        .filterKeys { !it.startsWith("org.gradle.") }
    val commandLineValues =
      settings.gradle.startParameter.projectProperties
        .filterKeys { !isSecretLike(it) }
    return fileValues + processValues + repositoryValues + commandLineValues
  }

  private fun loadProperties(file: File): Map<String, String> {
    if (!file.isFile) return emptyMap()
    val properties = Properties()
    file.inputStream().use(properties::load)
    val values = properties.stringPropertyNames().associateWith { properties.getProperty(it).trim() }
    val unsafeKey = values.keys.firstOrNull(::isSecretLike)
    require(unsafeKey == null) {
      "Environment configuration must not contain secret-like key '$unsafeKey'"
    }
    return values
  }

  private fun isSecretLike(key: String): Boolean =
    listOf("password", "secret", "token", "private-key", "private_key").any(
      key.lowercase()::contains,
    )

  private enum class EnvironmentName(
    val id: String,
  ) {
    LOCAL("local"),
    DEV("dev"),
    REGRESSION("regression"),
    STAGING("staging"),
    PRODUCTION("production"),
    ;

    companion object {
      fun parse(value: String): EnvironmentName =
        entries.firstOrNull { it.id == value.trim().lowercase() }
          ?: throw IllegalArgumentException(
            "Unsupported environment '$value'; expected ${entries.joinToString { it.id }}",
          )
    }
  }

  private companion object {
    const val ENVIRONMENT_NAME_KEY = "com.emme.environment.name"
    const val ENVIRONMENT_VALUES_KEY = "com.emme.environment.values"
  }
}
