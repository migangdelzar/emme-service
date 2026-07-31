package com.emme.buildlogic.provider.container

import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.io.File

abstract class ContainerRuntimeProvider :
  BuildService<ContainerRuntimeProvider.Params>,
  AutoCloseable {
  interface Params : BuildServiceParameters {
    val executable: Property<String>
  }

  abstract fun build(
    image: String,
    context: File,
    tags: List<String>,
  ): BuildResult

  abstract fun push(
    image: String,
    registry: String,
  ): PushResult

  abstract fun scan(
    image: String,
    severity: String,
    output: File,
  ): ScanResult

  override fun close() = Unit
}
