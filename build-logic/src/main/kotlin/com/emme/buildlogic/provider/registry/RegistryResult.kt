package com.emme.buildlogic.provider.registry

data class LoginResult(
  val success: Boolean,
  val registry: String,
)

data class ManifestResult(
  val digest: String,
  val size: Long,
)
