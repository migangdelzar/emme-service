package com.emme.buildlogic.environment

import com.emme.buildlogic.deployment.DeploymentTarget
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import javax.inject.Inject

/** Typed, non-secret environment configuration exposed to build capabilities. */
abstract class EnvironmentExtension
  @Inject
  constructor() {
    abstract val name: Property<EnvironmentName>
    abstract val target: Property<DeploymentTarget>
    abstract val runtime: Property<RuntimeKind>
    abstract val imageTag: Property<String>
    abstract val imageRegistry: Property<String>
    abstract val healthUrl: Property<String>
    abstract val kustomizeOverlay: Property<String>
    abstract val values: MapProperty<String, String>
    abstract val environmentsDirectory: DirectoryProperty

    /** Reads an arbitrary non-secret value from the resolved environment map. */
    fun value(
      key: String,
      defaultValue: String,
      vararg aliases: String,
    ): Provider<String> =
      values.map { resolved ->
        sequenceOf(key, *aliases)
          .mapNotNull { resolved[it]?.trim()?.takeIf(String::isNotBlank) }
          .firstOrNull()
          ?: defaultValue
      }
  }
