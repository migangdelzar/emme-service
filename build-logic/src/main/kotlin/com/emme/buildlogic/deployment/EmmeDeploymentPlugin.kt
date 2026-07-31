package com.emme.buildlogic.deployment

import com.emme.buildlogic.deployment.provider.ComposeProvider
import com.emme.buildlogic.deployment.provider.DeploymentProvider
import com.emme.buildlogic.deployment.provider.KubernetesProvider
import com.emme.buildlogic.deployment.task.DeployTask
import com.emme.buildlogic.deployment.task.DeploymentStatusTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Exec
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

class EmmeDeploymentPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    with(project) {
      val ext = extensions.create("emmeDeployment", EmmeDeploymentExtension::class.java)
      ext.deploymentDir.convention(rootProject.layout.projectDirectory.dir("deployment"))

      val target = ext.target.get()
      val providerClass =
        when (target.lowercase()) {
          "compose" -> ComposeProvider::class
          "kubernetes", "k3s", "k3d" -> KubernetesProvider::class
          else -> ComposeProvider::class
        }

      fun registerProvider(cls: Class<out DeploymentProvider>) =
        gradle.sharedServices.registerIfAbsent("emmeDeploymentProvider", cls) {
          parameters.profile.set(ext.profile)
          parameters.namespace.set(ext.namespace)
          parameters.deploymentDir.set(ext.deploymentDir)
          maxParallelUsages.set(1)
        }

      val deployment = registerProvider(providerClass.java)

      tasks.register("deployUp", DeployTask::class.java) {
        group = "deployment"
        description = "Start deployment"
        action.set("up")
        deploymentProvider.set(deployment)
      }
      tasks.register("deployDown", DeployTask::class.java) {
        group = "deployment"
        description = "Stop deployment"
        action.set("down")
        deploymentProvider.set(deployment)
      }
      tasks.register("deployApply", DeployTask::class.java) {
        group = "deployment"
        description = "Apply deployment changes"
        action.set("apply")
        deploymentProvider.set(deployment)
      }
      tasks.register("deployStatus", DeploymentStatusTask::class.java) {
        group = "deployment"
        description = "Check deployment status"
        deploymentProvider.set(deployment)
      }
      tasks.register("deployLogs", Exec::class.java) {
        group = "deployment"
        description = "Show deployment logs"
        executable = "echo"
        doFirst {
          val logs = deployment.get().logs(tail = 50)
          logger.lifecycle(logs)
        }
      }
    }
  }
}
