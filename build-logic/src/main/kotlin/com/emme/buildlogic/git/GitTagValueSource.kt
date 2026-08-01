package com.emme.buildlogic.git

import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import javax.inject.Inject

abstract class GitTagValueSource : ValueSource<String, ValueSourceParameters.None> {
  @get:Inject
  abstract val execOperations: ExecOperations

  override fun obtain(): String {
    val output = ByteArrayOutputStream()

    execOperations.exec {
      commandLine("git", "describe", "--tags", "--abbrev=0")
      standardOutput = output
      isIgnoreExitValue = true
    }

    val tag = output.toString(Charsets.UTF_8).trim()
    return tag.ifEmpty { "v0.0.0" }
  }
}
