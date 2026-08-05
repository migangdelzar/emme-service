package com.emme.buildlogic.environment

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import java.util.Properties

/** Lazily reads a non-secret environment properties file for Gradle caching. */
@Suppress("MaxLineLength")
abstract class EnvironmentPropertiesValueSource : ValueSource<Map<String, String>, EnvironmentPropertiesValueSource.Parameters> {
  interface Parameters : ValueSourceParameters {
    val file: RegularFileProperty
  }

  override fun obtain(): Map<String, String> {
    if (!parameters.file
        .get()
        .asFile.isFile
    ) {
      return emptyMap()
    }

    val properties = Properties()
    parameters.file
      .get()
      .asFile
      .inputStream()
      .use(properties::load)

    val values =
      properties.stringPropertyNames().associateWith { key ->
        properties.getProperty(key).trim()
      }
    val unsafeKey =
      values.keys.firstOrNull { key ->
        listOf("password", "secret", "token", "private-key", "private_key").any(key::contains)
      }
    require(unsafeKey == null) {
      "Environment properties must not contain secret-like key '$unsafeKey'"
    }
    return values
  }
}
