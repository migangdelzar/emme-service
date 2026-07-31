package com.emme.buildlogic.model

enum class QualityGateMode {
  STRICT,
  WARN,
  REPORT,
  ;

  companion object {
    fun fromString(value: String): QualityGateMode =
      entries.find { it.name.equals(value, ignoreCase = true) }
        ?: STRICT
  }
}
