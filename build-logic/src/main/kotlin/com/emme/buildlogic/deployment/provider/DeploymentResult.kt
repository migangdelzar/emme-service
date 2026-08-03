package com.emme.buildlogic.deployment.provider

data class DeployResult(
  val success: Boolean,
  val message: String,
)

data class StatusResult(
  val ready: Boolean,
  val pods: Int,
  val details: String,
)
