package com.emme.buildlogic.deployment

/** Resolves semantic deployment profiles to canonical Kubernetes overlays. */
internal object KubernetesDeploymentTarget {
  fun overlayName(
    profile: String,
    runtime: String,
  ): String {
    val target =
      when (profile.lowercase()) {
        "local", "test", "dev", "k3d" -> "k3d"
        "staging", "k3s-staging" -> "k3s-staging"
        "prod", "production", "k3s", "k3s-production" -> "k3s-production"
        else -> throw IllegalArgumentException("Unsupported Kubernetes profile '$profile'")
      }
    val normalizedRuntime = runtime.lowercase()
    require(normalizedRuntime == "jvm" || normalizedRuntime == "native") {
      "Unsupported Kubernetes runtime '$runtime'; expected 'jvm' or 'native'"
    }
    return "$target-$normalizedRuntime"
  }

  fun namespace(profile: String): String =
    when (profile.lowercase()) {
      "local", "test", "dev", "k3d" -> "emme-dev"
      "staging", "k3s-staging" -> "emme-staging"
      "prod", "production", "k3s", "k3s-production" -> "emme-prod"
      else -> throw IllegalArgumentException("Unsupported Kubernetes profile '$profile'")
    }
}
