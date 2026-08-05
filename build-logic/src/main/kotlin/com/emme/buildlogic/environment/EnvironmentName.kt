package com.emme.buildlogic.environment

/** Supported deployment environments for the platform. */
enum class EnvironmentName(
  val id: String,
) {
  LOCAL("local"),
  DEV("dev"),
  REGRESSION("regression"),
  STAGING("staging"),
  PRODUCTION("production"),
  ;

  companion object {
    fun parse(value: String): EnvironmentName =
      entries.firstOrNull { it.id == value.trim().lowercase() }
        ?: throw IllegalArgumentException(
          "Unsupported environment '$value'; expected ${entries.joinToString { it.id }}",
        )
  }
}
