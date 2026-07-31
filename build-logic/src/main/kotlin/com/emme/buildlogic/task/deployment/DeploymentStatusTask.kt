package com.emme.buildlogic.task.deployment

import com.emme.buildlogic.provider.deployment.DeploymentProvider
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

abstract class DeploymentStatusTask : DefaultTask() {
  @get:Internal
  abstract val deploymentProvider: Property<DeploymentProvider>

  @TaskAction
  fun check() {
    val status = deploymentProvider.get().status()
    logger.lifecycle("Deployment status: ready={}, pods={}", status.ready, status.pods)
    if (!status.ready) logger.warn("Deployment not ready:\n{}", status.details)
  }
}
