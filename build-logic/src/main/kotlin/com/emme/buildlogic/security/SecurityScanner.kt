package com.emme.buildlogic.security

enum class SecurityScanner(
  val executable: String,
) {
  TRIVY("trivy"),
  GRYPE("grype"),
  ;

  companion object {
    fun fromString(value: String): SecurityScanner =
      entries.find { it.name.equals(value, ignoreCase = true) }
        ?: TRIVY
  }
}
