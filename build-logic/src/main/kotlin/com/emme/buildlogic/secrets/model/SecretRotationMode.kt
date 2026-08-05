package com.emme.buildlogic.secrets.model

/** Controls whether a provider may persist a generated secret. */
enum class SecretRotationMode(
  val id: String,
) {
  DRY_RUN("dry-run"),
  APPLY("apply"),
  ;

  companion object {
    fun parse(value: String): SecretRotationMode =
      entries.firstOrNull { it.id == value.trim().lowercase() }
        ?: throw IllegalArgumentException(
          "Unsupported secret rotation mode '$value'; expected ${entries.joinToString { it.id }}",
        )
  }
}
