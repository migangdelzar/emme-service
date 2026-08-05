package com.emme.buildlogic.secrets

/** Secret source selected for validation; values remain owned by the source. */
enum class SecretProviderKind(
  val id: String,
) {
  AUTO("auto"),
  ENVIRONMENT("environment"),
  BITWARDEN("bitwarden"),
  GITHUB_ACTIONS("github-actions"),
  KUBERNETES("kubernetes"),
  ;

  companion object {
    fun parse(value: String): SecretProviderKind =
      entries.firstOrNull { it.id == value.trim().lowercase() }
        ?: throw IllegalArgumentException(
          "Unsupported secret provider '$value'; expected ${entries.joinToString { it.id }}",
        )
  }
}
