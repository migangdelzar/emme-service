package com.emme.buildlogic.secrets.task

import org.gradle.api.DefaultTask
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/** Verifies non-secret Kubernetes/secret-manager reference declarations. */
@DisableCachingByDefault(because = "References describe external secret state")
abstract class VerifySecretReferencesTask : DefaultTask() {
  @get:Input
  abstract val references: MapProperty<String, String>

  @TaskAction
  fun verify() {
    val values = references.get()
    val invalid = values.filter { (name, reference) -> name.isBlank() || reference.isBlank() }
    require(invalid.isEmpty()) {
      "Secret references must contain non-empty logical names and references"
    }
    logger.lifecycle("Secret references verified: {} declarations", values.size)
  }
}
