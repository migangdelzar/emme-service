package com.emme.buildlogic.publishing.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.time.Instant
import java.util.Properties

abstract class GenerateBuildInfoTask : DefaultTask() {
  @get:Input
  abstract val version: Property<String>

  @get:Input
  abstract val commit: Property<String>

  @get:Input
  abstract val branch: Property<String>

  @get:Input
  abstract val channel: Property<String>

  @get:Input
  abstract val buildTimestamp: Property<String>

  @get:OutputFile
  abstract val outputFile: RegularFileProperty

  init {
    // No convention: resolved lazily in @TaskAction for reproducibility within a build
  }

  @TaskAction
  fun generate() {
    val props =
      Properties().apply {
        setProperty("build.version", version.get())
        setProperty("build.commit", commit.get())
        setProperty("build.branch", branch.get())
        setProperty("build.channel", channel.get())
        setProperty("build.timestamp", buildTimestamp.orElse(Instant.now().toString()).get())
      }

    outputFile.get().asFile.apply {
      parentFile.mkdirs()
      outputStream().use { props.store(it, "Emme Build Info") }
    }
  }
}
