package com.emme.buildlogic.registry

data class LoginResult(
  val success: Boolean,
  val registry: String,
)

data class ManifestResult(
  val digest: String,
  val size: Long,
)

data class RegistryPushResult(
  val manifest: String,
)
