package com.emme.buildlogic.extension

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import javax.inject.Inject

abstract class EmmeDeploymentExtension
  @Inject
  constructor(
    providers: ProviderFactory,
  ) {
    /** Target: "compose", "k3d", or "kubernetes" */
    abstract val target: Property<String>

    /** Profile/environment: "local", "test", "staging", "production" */
    abstract val profile: Property<String>

    /** Namespace for K8s deployments */
    abstract val namespace: Property<String>

    /** Extra environment variables passed to all deployment commands */
    abstract val environment: MapProperty<String, String>

    /** Directory containing deployment configs (default: deployment/) */
    abstract val deploymentDir: DirectoryProperty

    init {
      target.convention(
        providers
          .gradleProperty("emme.deployment.target")
          .orElse(providers.environmentVariable("EMME_DEPLOYMENT_TARGET"))
          .orElse("compose"),
      )
      profile.convention(
        providers
          .gradleProperty("emme.deployment.profile")
          .orElse(providers.environmentVariable("EMME_DEPLOYMENT_PROFILE"))
          .orElse("local"),
      )
      namespace.convention(profile.map { "emme-$it" })
    }
  }
