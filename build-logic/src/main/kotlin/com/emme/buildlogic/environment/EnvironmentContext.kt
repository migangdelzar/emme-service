package com.emme.buildlogic.environment

/** Immutable, non-secret environment snapshot shared with all build capabilities. */
data class EnvironmentContext(
  val name: EnvironmentName,
  val values: Map<String, String>,
) {
  init {
    require(values.keys.none(::isSecretLike)) {
      "Environment context must not contain secret-like properties"
    }
  }

  fun value(key: String): String? = values[key]

  companion object {
    fun isSecretLike(key: String): Boolean =
      listOf("password", "secret", "token", "private-key", "private_key").any(
        key.lowercase()::contains,
      )
  }
}
