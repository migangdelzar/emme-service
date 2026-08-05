package com.emme.buildlogic.secrets.model

/** Provider-neutral outcome; it intentionally contains no secret values. */
data class SecretRotationReport(
  val provider: String,
  val mode: SecretRotationMode,
  val entries: List<Entry>,
) {
  data class Entry(
    val name: String,
    val status: Status,
  )

  enum class Status {
    PLANNED,
    ROTATED,
    UNSUPPORTED,
  }
}
