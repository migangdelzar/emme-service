package com.emme.buildlogic.task.publishing

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.time.Instant

abstract class GenerateReleaseManifest : DefaultTask() {
  @get:Input
  abstract val version: Property<String>

  @get:Input
  abstract val channel: Property<String>

  @get:Input
  abstract val commit: Property<String>

  @get:Input
  abstract val registry: Property<String>

  @get:Input
  abstract val releaseTimestamp: Property<String>

  @get:OutputFile
  abstract val manifestFile: RegularFileProperty

  init {
    // No convention: resolved lazily in @TaskAction for reproducibility
  }

  @TaskAction
  fun generate() {
    val yaml =
      buildString {
        appendLine("release:")
        appendLine("  version: \"${version.get()}\"")
        appendLine("  channel: \"${channel.get()}\"")
        appendLine("  commit: \"${commit.get()}\"")
        appendLine("  registry: \"${registry.get()}\"")
        appendLine("  timestamp: \"${releaseTimestamp.orElse(Instant.now().toString()).get()}\"")
      }

    manifestFile.get().asFile.apply {
      parentFile.mkdirs()
      writeText(yaml)
    }
  }
}
