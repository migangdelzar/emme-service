package com.emme.buildlogic.registry

enum class RegistryTarget(
  val defaultUrl: String,
) {
  LOCAL("localhost:5000"),
  GHCR("ghcr.io"),
  ECR("public.ecr.aws"),
  ;

  companion object {
    fun fromString(value: String): RegistryTarget =
      entries.find { it.name.equals(value, ignoreCase = true) }
        ?: LOCAL
  }
}
