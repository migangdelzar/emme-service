package com.emme.buildlogic.task.deployment

import com.emme.buildlogic.provider.deployment.DeploymentProvider
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

abstract class DeployTask : DefaultTask() {
  @get:Input
  abstract val action: Property<String>

  @get:Internal
  abstract val deploymentProvider: Property<DeploymentProvider>

  @TaskAction
  fun deploy() {
    val result =
      when (action.get()) {
        "up" -> deploymentProvider.get().up()
        "down" -> deploymentProvider.get().down()
        "apply" -> deploymentProvider.get().apply()
        else -> throw IllegalArgumentException("Unknown deploy action: ${action.get()}")
      }
    if (!result.success) throw RuntimeException("Deployment failed: ${result.message}")
    logger.lifecycle("Deployment {} successful: {}", action.get(), result.message)
  }
}
