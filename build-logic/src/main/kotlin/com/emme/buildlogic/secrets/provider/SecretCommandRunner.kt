package com.emme.buildlogic.secrets.provider

/** A process boundary used by provider adapters and replaceable in tests. */
fun interface SecretCommandRunner {
  fun run(
    command: List<String>,
    stdin: String?,
  ): SecretCommandResult

  companion object {
    fun system(environment: Map<String, String>): SecretCommandRunner =
      SecretCommandRunner { command, stdin ->
        val process =
          ProcessBuilder(command)
            .redirectErrorStream(true)
            .apply { environment().putAll(environment) }
            .start()
        stdin?.let { process.outputStream.bufferedWriter().use { writer -> writer.write(it) } }
        val output = process.inputStream.bufferedReader().use { it.readText() }
        SecretCommandResult(process.waitFor(), output)
      }
  }
}

data class SecretCommandResult(
  val exitCode: Int,
  val stdout: String,
) {
  fun requireSuccess(): SecretCommandResult =
    apply {
      check(exitCode == 0) { "Secret provider command failed" }
    }
}
