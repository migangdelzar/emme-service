package com.emme.buildlogic.container

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import javax.inject.Inject

abstract class EmmeContainerExtension
  @Inject
  constructor(
    providers: ProviderFactory,
  ) {
    abstract val enabled: Property<Boolean>
    abstract val runtime: Property<ContainerRuntime>
    abstract val imageName: Property<String>
    abstract val imageTags: ListProperty<String>
    abstract val contextDirectory: DirectoryProperty
    abstract val push: Property<Boolean>
    abstract val baseImage: Property<String>
    abstract val containerPort: Property<Int>
    abstract val jvmFlags: ListProperty<String>
    abstract val platforms: ListProperty<String>

    init {
      enabled.convention(false)
      runtime.convention(
        providers
          .gradleProperty("emme.container.runtime")
          .orElse(providers.environmentVariable("EMME_CONTAINER_RUNTIME"))
          .map { ContainerRuntime.valueOf(it.uppercase()) }
          .orElse(ContainerRuntime.DOCKER),
      )
      imageName.convention(
        providers
          .gradleProperty("emme.container.imageName")
          .orElse(providers.environmentVariable("EMME_CONTAINER_IMAGE_NAME"))
          .orElse(""),
      )
      imageTags.convention(listOf("latest"))
      push.convention(false)
      baseImage.convention("eclipse-temurin:25-jre")
      containerPort.convention(8081)
      jvmFlags.convention(
        listOf("-XX:+UseZGC", "-XX:MaxRAMPercentage=75", "--enable-preview"),
      )
      platforms.convention(emptyList())
    }
  }
