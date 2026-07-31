package com.emme.buildlogic.provider.security

import java.io.ByteArrayOutputStream
import java.io.File

abstract class TrivyProvider : SecurityScannerProvider() {
  override fun scan(
    image: String,
    output: File,
  ): SecurityScanResult {
    output.parentFile.mkdirs()

    val sev = parameters.severity.get()
    val scanner = parameters.scanner.get()

    val execOutput =
      executeCommand(
        listOf(
          scanner,
          "image",
          "--severity",
          sev,
          "--format",
          "sarif",
          "--output",
          output.absolutePath,
          "--exit-code",
          "0",
          image,
        ),
      )

    val critical = execOutput.lines().count { it.contains("\"level\":\"error\"") }
    val high = execOutput.lines().count { it.contains("\"level\":\"warning\"") }

    return SecurityScanResult(
      vulnerabilities = critical + high,
      critical = critical,
      high = high,
      reportPath = output.absolutePath,
    )
  }

  private fun executeCommand(args: List<String>): String {
    val output = ByteArrayOutputStream()
    val process = ProcessBuilder(args).redirectErrorStream(true).start()
    process.inputStream.copyTo(output)
    process.waitFor()
    return output.toString(Charsets.UTF_8)
  }
}
