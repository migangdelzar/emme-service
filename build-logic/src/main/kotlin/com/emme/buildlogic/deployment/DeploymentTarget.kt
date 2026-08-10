package com.emme.buildlogic.deployment

enum class DeploymentTarget(
  val displayName: String,
) {
  COMPOSE("Docker Compose"),
  K3D("k3d (Docker-based K3s)"),
  K3S("K3s (bare-metal)"),
  KUBERNETES("Kubernetes (production)"),
  ;

  companion object {
    fun fromString(value: String): DeploymentTarget =
      entries.find { it.name.equals(value, ignoreCase = true) }
        ?: throw IllegalArgumentException(
          "Unsupported deployment target '$value'. " +
            "Supported targets: ${entries.joinToString { it.name.lowercase() }}",
        )
  }
}
