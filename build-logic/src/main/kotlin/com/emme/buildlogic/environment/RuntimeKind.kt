package com.emme.buildlogic.environment

/** Runtime artifact selected independently from the deployment environment. */
enum class RuntimeKind(
  val id: String,
) {
  JVM("jvm"),
  NATIVE("native"),
  ;

  companion object {
    fun parse(value: String): RuntimeKind =
      entries.firstOrNull { it.id == value.trim().lowercase() }
        ?: throw IllegalArgumentException("Unsupported runtime '$value'; expected jvm or native")
  }
}
