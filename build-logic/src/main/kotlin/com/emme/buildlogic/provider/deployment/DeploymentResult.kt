package com.emme.buildlogic.provider.deployment

data class DeployResult(
  val success: Boolean,
  val message: String,
)

data class StatusResult(
  val ready: Boolean,
  val pods: Int,
  val details: String,
)
