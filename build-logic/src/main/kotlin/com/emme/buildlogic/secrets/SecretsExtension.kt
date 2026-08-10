package com.emme.buildlogic.secrets

import com.emme.buildlogic.secrets.model.SecretRotationMode
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

/** Public DSL for safe secret requirements and external references. */
abstract class SecretsExtension
  @Inject
  constructor() {
    abstract val provider: Property<SecretProviderKind>
    abstract val required: ListProperty<String>
    abstract val references: MapProperty<String, String>
    abstract val manifest: RegularFileProperty
    abstract val rotationMode: Property<SecretRotationMode>

    fun required(name: String) {
      required.add(name)
    }

    fun reference(
      name: String,
      reference: String,
    ) {
      references.put(name, reference)
    }
  }
