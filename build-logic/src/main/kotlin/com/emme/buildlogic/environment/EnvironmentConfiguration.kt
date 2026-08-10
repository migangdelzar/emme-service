package com.emme.buildlogic.environment

import com.emme.buildlogic.deployment.DeploymentTarget

/** Immutable, non-secret configuration consumed by build capabilities. */
data class EnvironmentConfiguration(
  val name: EnvironmentName,
  val target: DeploymentTarget,
  val runtime: RuntimeKind,
  val imageTag: String,
  val imageRegistry: String,
  val healthUrl: String,
  val kustomizeOverlay: String,
)
