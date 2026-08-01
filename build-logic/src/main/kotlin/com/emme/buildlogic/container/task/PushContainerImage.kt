package com.emme.buildlogic.container.task

import com.emme.buildlogic.container.provider.ContainerRuntimeProvider
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

abstract class PushContainerImage : DefaultTask() {
  @get:Input
  abstract val imageName: Property<String>

  @get:Input
  abstract val registry: Property<String>

  @get:Internal
  abstract val runtimeService: Property<ContainerRuntimeProvider>

  init {
    registry.convention("")
    doNotTrackState(
      "Container push creates external state.",
    )
  }

  @TaskAction
  fun push() {
    val result = runtimeService.get().push(imageName.get(), registry.get())
    logger.lifecycle("Container pushed: {}", result.manifest)
  }
}
