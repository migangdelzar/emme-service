package com.emme.buildlogic.environment.task

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/** Prints non-secret environment metadata for diagnostics. */
abstract class EnvironmentReportTask : DefaultTask() {
  @get:Input
  abstract val environmentName: Property<String>

  @get:Input
  abstract val target: Property<String>

  @get:Input
  abstract val runtime: Property<String>

  @get:Input
  abstract val imageTag: Property<String>

  @get:Input
  abstract val imageRegistry: Property<String>

  @get:Input
  abstract val healthUrl: Property<String>

  @TaskAction
  fun report() {
    logger.lifecycle("Environment: {}", environmentName.get())
    logger.lifecycle("Target: {}", target.get())
    logger.lifecycle("Runtime: {}", runtime.get())
    logger.lifecycle("Image: {}/{}", imageRegistry.get(), imageTag.get())
    logger.lifecycle("Health URL: {}", healthUrl.get())
  }
}
