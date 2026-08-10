package com.emme.buildlogic.environment.task

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/** Validates the resolved environment without exposing secret material. */
abstract class VerifyEnvironmentTask : DefaultTask() {
  @get:Input
  abstract val environmentName: Property<String>

  @get:Input
  abstract val target: Property<String>

  @get:Input
  abstract val runtime: Property<String>

  @get:Input
  abstract val overlay: Property<String>

  @TaskAction
  fun verify() {
    require(environmentName.get() in setOf("local", "dev", "regression", "staging", "production")) {
      "Unsupported environment '${environmentName.get()}'"
    }
    require(target.get() in setOf("compose", "k3d", "k3s", "kubernetes")) {
      "Unsupported deployment target '${target.get()}'"
    }
    require(runtime.get() in setOf("jvm", "native")) {
      "Unsupported runtime '${runtime.get()}'"
    }
    require(overlay.get().isNotBlank()) { "Kustomize overlay must not be blank" }
    logger.lifecycle(
      "Environment verified: name={}, target={}, runtime={}, overlay={}",
      environmentName.get(),
      target.get(),
      runtime.get(),
      overlay.get(),
    )
  }
}
