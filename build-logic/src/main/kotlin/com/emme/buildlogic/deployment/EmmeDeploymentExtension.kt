package com.emme.buildlogic.deployment

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
    /** Deployment strategy selected by the project or CI environment. */
    abstract val target: Property<DeploymentTarget>

    /** Profile/environment: local, test, staging, or production. */
    abstract val profile: Property<String>

    /** Runtime variant selected by deployment: "jvm" or "native". */
    abstract val runtime: Property<String>

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
          .map(DeploymentTarget::fromString)
          .orElse(DeploymentTarget.COMPOSE),
      )
      profile.convention(
        providers
          .gradleProperty("emme.deployment.profile")
          .orElse(providers.environmentVariable("EMME_DEPLOYMENT_PROFILE"))
          .orElse("local"),
      )
      namespace.convention(profile.map(KubernetesDeploymentTarget::namespace))
      runtime.convention(
        providers
          .gradleProperty("emme.deployment.runtime")
          .orElse(providers.environmentVariable("EMME_DEPLOYMENT_RUNTIME"))
          .orElse("jvm"),
      )
    }
  }
