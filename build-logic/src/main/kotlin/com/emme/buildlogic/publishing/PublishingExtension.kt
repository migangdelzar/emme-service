package com.emme.buildlogic.publishing

import com.emme.buildlogic.model.ReleaseChannel
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import javax.inject.Inject

abstract class PublishingExtension
  @Inject
  constructor(
    providers: ProviderFactory,
  ) {
    abstract val enabled: Property<Boolean>
    abstract val channel: Property<ReleaseChannel>
    abstract val version: Property<String>
    abstract val registry: Property<String>
    abstract val signArtifacts: Property<Boolean>
    abstract val signingKeyId: Property<String>
    abstract val platforms: ListProperty<String>

    init {
      enabled.convention(false)
      channel.convention(ReleaseChannel.SNAPSHOT)
      signArtifacts.convention(false)
      signingKeyId.convention(
        providers.gradleProperty("emme.publishing.signingKeyId").orElse(""),
      )
      platforms.convention(listOf("linux/amd64", "linux/arm64"))
    }
  }
