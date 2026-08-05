package com.emme.buildlogic.secrets.task

import com.emme.buildlogic.secrets.SecretProviderKind
import com.emme.buildlogic.secrets.provider.SecretProviderFactory
import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/** Verifies secret availability without making values Gradle inputs or outputs. */
@DisableCachingByDefault(because = "Secret values must never enter build-cache state")
abstract class VerifySecretsTask : DefaultTask() {
  @get:Input
  abstract val requiredNames: ListProperty<String>

  @get:Input
  abstract val provider: Property<SecretProviderKind>

  @TaskAction
  fun verify() {
    val required = requiredNames.get().toSet()
    val actual = SecretProviderFactory.create(provider.get(), System.getenv())
    val missing = actual.validate(required)
    require(missing.isEmpty()) {
      "Missing required secrets for ${actual.kind.id}: ${missing.sorted().joinToString() }"
    }
    logger.lifecycle("Secret provider verified: {} ({} required names)", actual.kind.id, required.size)
  }
}
