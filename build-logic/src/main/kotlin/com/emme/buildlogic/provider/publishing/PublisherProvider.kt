package com.emme.buildlogic.provider.publishing

import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.io.File

abstract class PublisherProvider : BuildService<PublisherProvider.Params> {
  interface Params : BuildServiceParameters {
    val keyId: Property<String>
    val signArtifacts: Property<Boolean>
  }

  abstract fun sign(
    artifact: File,
    keyId: String,
  ): SignResult

  abstract fun publish(
    artifact: File,
    registry: String,
  ): PublishResult
}
