package com.emme.buildlogic.container.task

import com.emme.buildlogic.container.provider.ContainerRuntimeProvider
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class BuildContainerImage : DefaultTask() {
  @get:Input
  abstract val imageName: Property<String>

  @get:Input
  abstract val tags: ListProperty<String>

  @get:InputDirectory
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val contextDirectory: DirectoryProperty

  @get:Internal
  abstract val runtimeService: Property<ContainerRuntimeProvider>

  init {
    tags.convention(listOf("latest"))
    doNotTrackState(
      "Container image is created in an external container runtime.",
    )
  }

  @TaskAction
  fun build() {
    val result =
      runtimeService.get().build(
        image = imageName.get(),
        context = contextDirectory.get().asFile,
        tags = tags.get(),
      )
    logger.lifecycle("Container built: {} (digest: {})", result.imageId, result.digest)
  }
}
