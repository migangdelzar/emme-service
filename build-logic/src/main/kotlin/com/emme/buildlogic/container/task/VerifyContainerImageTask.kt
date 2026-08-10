package com.emme.buildlogic.container.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.ByteArrayOutputStream

abstract class VerifyContainerImageTask : DefaultTask() {
  private companion object {
    const val MAX_VULNERABILITY_OUTPUT_LENGTH = 2000
  }

  @get:Input
  abstract val imageName: Property<String>

  @get:Input
  abstract val severity: Property<String>

  @get:OutputFile
  abstract val reportFile: RegularFileProperty

  init {
    severity.convention("HIGH,CRITICAL")
  }

  @TaskAction
  fun verify() {
    val image = imageName.get()
    val sev = severity.get()
    val report = reportFile.get().asFile

    report.parentFile.mkdirs()

    logger.lifecycle("Scanning image: {}", image)

    val output = ByteArrayOutputStream()
    val process =
      ProcessBuilder(
        "trivy",
        "image",
        "--severity",
        sev,
        "--exit-code",
        "1",
        "--format",
        "sarif",
        "--output",
        report.absolutePath,
        image,
      ).redirectErrorStream(true)
        .start()

    process.inputStream.copyTo(output)
    val exitCode = process.waitFor()
    val text = output.toString(Charsets.UTF_8)

    if (exitCode != 0) {
      logger.warn(
        "Container vulnerabilities found:\n{}",
        text.take(MAX_VULNERABILITY_OUTPUT_LENGTH),
      )
    } else {
      logger.lifecycle("Container scan clean.")
    }
  }
}
