package com.emme.buildlogic.deployment

import com.emme.buildlogic.deployment.provider.ComposeProvider
import com.emme.buildlogic.deployment.provider.DeploymentProvider
import com.emme.buildlogic.deployment.provider.KubernetesProvider
import com.emme.buildlogic.deployment.task.DeployTask
import com.emme.buildlogic.deployment.task.DeploymentStatusTask
import com.emme.buildlogic.environment.EnvironmentExtension
import com.emme.buildlogic.environment.EnvironmentPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Exec
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register

class DeploymentPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.pluginManager.apply(EnvironmentPlugin::class.java)
    val environment = project.extensions.getByType<EnvironmentExtension>()
    val extension =
      project.extensions.create("emmeDeployment", DeploymentExtension::class.java)
    extension.target.convention(environment.target)
    extension.profile.convention(environment.name.map { it.id })
    extension.runtime.convention(environment.runtime.map { it.id })
    extension.namespace.convention(extension.profile.map(KubernetesDeploymentTarget::namespace))
    extension.deploymentDir.convention(
      project.rootProject.layout.projectDirectory
        .dir("deployment"),
    )

    val deployment = project.registerDeploymentProvider(extension)
    project.registerDeploymentTasks(deployment)
  }

  private fun Project.registerDeploymentProvider(extension: DeploymentExtension): Provider<DeploymentProvider> {
    val composeDeployment =
      gradle.sharedServices.registerIfAbsent("emmeComposeDeploymentProvider", ComposeProvider::class.java) {
        parameters.profile.set(extension.profile)
        parameters.runtime.set(extension.runtime)
        parameters.namespace.set(extension.namespace)
        parameters.deploymentDir.set(extension.deploymentDir)
        maxParallelUsages.set(1)
      }

    val kubernetesDeployment =
      gradle.sharedServices.registerIfAbsent(
        "emmeKubernetesDeploymentProvider",
        KubernetesProvider::class.java,
      ) {
        parameters.profile.set(extension.profile)
        parameters.runtime.set(extension.runtime)
        parameters.namespace.set(extension.namespace)
        parameters.deploymentDir.set(extension.deploymentDir)
        maxParallelUsages.set(1)
      }

    return extension.target.map { target ->
      when (target) {
        DeploymentTarget.COMPOSE -> composeDeployment.get()

        DeploymentTarget.K3D,
        DeploymentTarget.K3S,
        DeploymentTarget.KUBERNETES,
        -> kubernetesDeployment.get()
      }
    }
  }

  private fun Project.registerDeploymentTasks(deployment: Provider<DeploymentProvider>) {
    listOf(
      Triple("deployUp", "up", "Start deployment"),
      Triple("deployDown", "down", "Stop deployment"),
      Triple("deployApply", "apply", "Apply deployment changes"),
    ).forEach { (taskName, action, description) ->
      tasks.register(taskName, DeployTask::class.java) {
        group = "deployment"
        this.description = description
        this.action.set(action)
        deploymentProvider.set(deployment)
      }
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
