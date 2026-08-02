package com.emme.buildlogic.container

import com.emme.buildlogic.container.provider.ContainerRuntimeProvider
import com.emme.buildlogic.container.provider.DockerProvider
import com.emme.buildlogic.container.provider.PodmanProvider
import com.emme.buildlogic.container.task.BuildContainerImageTask
import com.emme.buildlogic.container.task.PushContainerImageTask
import com.emme.buildlogic.container.task.VerifyContainerImageTask
import com.emme.buildlogic.core.TaskNames
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Exec
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

class EmmeContainerPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    with(project) {
      val extension = extensions.create("emmeContainer", EmmeContainerExtension::class.java)
      extension.contextDirectory.convention(layout.projectDirectory)

      val dockerRuntime =
        gradle.sharedServices.registerIfAbsent(
          "emmeDockerRuntime",
          DockerProvider::class.java,
        ) {
          parameters.executable.set("docker")
          maxParallelUsages.set(2)
        }

      val podmanRuntime =
        gradle.sharedServices.registerIfAbsent(
          "emmePodmanRuntime",
          PodmanProvider::class.java,
        ) {
          parameters.executable.set("podman")
          maxParallelUsages.set(2)
        }

      val runtime: Provider<ContainerRuntimeProvider> =
        extension.runtime.map { selectedRuntime ->
          when (selectedRuntime) {
            ContainerRuntime.DOCKER -> dockerRuntime.get()
            ContainerRuntime.PODMAN -> podmanRuntime.get()
          }
        }

      tasks.register(TaskNames.CONTAINER_BUILD, BuildContainerImageTask::class.java) {
        group = "container"
        imageName.set(extension.imageName)
        contextDirectory.set(extension.contextDirectory)
        runtimeService.set(runtime)
        onlyIf { extension.enabled.get() }
      }

      tasks.register(TaskNames.CONTAINER_PUSH, PushContainerImageTask::class.java) {
        group = "container"
        imageName.set(extension.imageName)
        runtimeService.set(runtime)
        onlyIf { extension.enabled.get() && extension.push.get() }
      }

      tasks.register(TaskNames.CONTAINER_VERIFY, VerifyContainerImageTask::class.java) {
        group = "container"
        imageName.set(extension.imageName)
        severity.set(
          providers
            .gradleProperty("emme.container.scan.severity")
            .orElse(providers.environmentVariable("EMME_CONTAINER_SCAN_SEVERITY"))
            .orElse("HIGH,CRITICAL"),
        )
        reportFile.set(layout.buildDirectory.file("reports/trivy/container-scan.sarif"))
        onlyIf { extension.enabled.get() }
      }

      tasks.register(TaskNames.CONTAINER_MULTI_ARCH, Exec::class.java) {
        group = "container"
        description = "Build multi-architecture container image"
        inputs.property("imageName", extension.imageName)
        inputs.property("platforms", extension.platforms)
        inputs.dir(extension.contextDirectory)
        executable = "docker"
        doFirst {
          val platforms = extension.platforms.get()
          val image = extension.imageName.get()
          val context =
            extension.contextDirectory
              .get()
              .asFile.absolutePath
          setArgs(
            mutableListOf<String>().also { list ->
              list.add("buildx")
              list.add("build")
              platforms.forEach {
                list.add("--platform")
                list.add(it)
              }
              list.addAll(listOf("-t", image, context))
              if (extension.push.get()) list.add("--push")
            },
          )
        }
        onlyIf { extension.enabled.get() && extension.platforms.get().size > 1 }
      }
    }
  }
}
