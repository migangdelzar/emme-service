package com.emme.buildlogic.container.provider

data class BuildResult(
  val imageId: String,
  val digest: String,
)

data class PushResult(
  val manifest: String,
)

data class ScanResult(
  val vulnerabilities: Int,
  val reportPath: String,
) {
  fun isClean(maxSeverity: String = "CRITICAL"): Boolean = vulnerabilities == 0
}
