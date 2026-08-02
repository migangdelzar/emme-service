package com.emme.buildlogic.publishing.task

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

abstract class VerifyReleaseVersionTask : DefaultTask() {
  @get:Input
  abstract val version: Property<String>

  @TaskAction
  fun verify() {
    val v = version.get()
    val semverRegex =
      Regex(
        """^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(-[a-zA-Z0-9.]+)?(\+[a-zA-Z0-9.]+)?$""",
      )

    check(semverRegex.matches(v)) {
      "Version '$v' does not match semantic versioning format."
    }

    logger.lifecycle("Version '{}' is valid.", v)
  }
}
