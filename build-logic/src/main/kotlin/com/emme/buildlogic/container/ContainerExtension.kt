package com.emme.buildlogic.container

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

abstract class ContainerExtension {
  abstract val enabled: Property<Boolean>
  abstract val runtime: Property<ContainerRuntime>
  abstract val imageName: Property<String>
  abstract val imageTags: ListProperty<String>
  abstract val contextDirectory: DirectoryProperty
  abstract val push: Property<Boolean>
  abstract val baseImage: Property<String>
  abstract val containerPort: Property<Int>
  abstract val jvmFlags: ListProperty<String>
  abstract val platforms: ListProperty<String>
}
