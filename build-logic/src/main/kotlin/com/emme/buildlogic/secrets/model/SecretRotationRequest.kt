package com.emme.buildlogic.secrets.model

/** A logical secret and the provider-owned location that stores it. */
data class SecretRotationRequest(
  val name: String,
  val reference: String,
  val length: Int = DEFAULT_LENGTH,
) {
  init {
    require(name.matches(LOGICAL_NAME)) {
      "Secret name must use uppercase letters, digits, and underscores: $name"
    }
    require(reference.isNotBlank()) { "Secret reference must not be blank" }
    require(length in MIN_LENGTH..MAX_LENGTH) {
      "Secret length must be between $MIN_LENGTH and $MAX_LENGTH characters"
    }
  }

  private companion object {
    private const val DEFAULT_LENGTH = 32
    private const val MIN_LENGTH = 16
    private const val MAX_LENGTH = 256
    private val LOGICAL_NAME = Regex("[A-Z][A-Z0-9_]*")
  }
}
