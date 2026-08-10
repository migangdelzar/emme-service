package com.emme.buildlogic.deployment

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property

abstract class DeploymentExtension {
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
}
