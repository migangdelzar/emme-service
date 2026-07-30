package com.emme.buildlogic.plugin

import com.emme.buildlogic.extension.EmmeContainerExtension
import com.emme.buildlogic.internal.TaskNames
import com.emme.buildlogic.provider.container.ContainerRuntimeProvider
import com.emme.buildlogic.provider.container.DockerProvider
import com.emme.buildlogic.task.container.BuildContainerImage
import com.emme.buildlogic.task.container.PushContainerImage
import com.emme.buildlogic.task.container.VerifyContainerImage
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Exec
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

class EmmeContainerPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        with(project) {
            val extension = extensions.create("emmeContainer", EmmeContainerExtension::class.java)

            val runtimeDriver = extension.runtime.map { it.name.lowercase() }.get()
            // TODO: add PodmanProvider when implemented; both currently map to DockerProvider
            val runtimeClass = when (runtimeDriver) {
                "podman" -> DockerProvider::class.java
                else -> DockerProvider::class.java
            }

            val runtime = gradle.sharedServices.registerIfAbsent(
                "emmeContainerRuntime", runtimeClass
            ) {
                parameters.executable.set(runtimeDriver)
                maxParallelUsages.set(2)
            }

            tasks.register(TaskNames.CONTAINER_BUILD, BuildContainerImage::class.java) {
                group = "container"
                imageName.set(extension.imageName)
                contextDirectory.set(extension.contextDirectory)
                runtimeService.set(runtime)
                onlyIf { extension.enabled.get() }
            }

            tasks.register(TaskNames.CONTAINER_PUSH, PushContainerImage::class.java) {
                group = "container"
                imageName.set(extension.imageName)
                runtimeService.set(runtime)
                onlyIf { extension.enabled.get() && extension.push.get() }
            }

            tasks.register(TaskNames.CONTAINER_VERIFY, VerifyContainerImage::class.java) {
                group = "container"
                imageName.set(extension.imageName)
                severity.set(
                    providers.gradleProperty("emme.container.scan.severity")
                        .orElse(providers.environmentVariable("EMME_CONTAINER_SCAN_SEVERITY"))
                        .orElse("HIGH,CRITICAL")
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
                    val context = extension.contextDirectory.get().asFile.absolutePath
                    setArgs(mutableListOf<String>().also { list ->
                        list.add("buildx"); list.add("build")
                        platforms.forEach { list.add("--platform"); list.add(it) }
                        list.addAll(listOf("-t", image, context))
                        if (extension.push.get()) list.add("--push")
                    })
                }
                onlyIf { extension.enabled.get() && extension.platforms.get().size > 1 }
            }
        }
    }
}
