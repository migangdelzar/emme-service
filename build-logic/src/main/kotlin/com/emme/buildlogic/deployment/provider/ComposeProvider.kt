package com.emme.buildlogic.deployment.provider

import java.io.ByteArrayOutputStream
import java.io.File

abstract class ComposeProvider : DeploymentProvider() {
  private fun composeFile(): File =
    parameters.deploymentDir
      .file("compose/compose.${parameters.profile.get()}.yml")
      .get()
      .asFile

  private fun envFile(): File =
    parameters.deploymentDir
      .file("compose/env/${parameters.profile.get()}.env")
      .get()
      .asFile

  override fun up(): DeployResult {
    val compose = composeFile()
    if (!compose.exists()) return DeployResult(false, "Compose file not found: ${compose.absolutePath}")
    val args = mutableListOf("compose", "-f", compose.absolutePath, "up", "-d")
    val env = envFile()
    if (env.exists()) args.addAll(listOf("--env-file", env.absolutePath))
    return execute("docker", args)
  }

  override fun down(): DeployResult {
    val compose = composeFile()
    if (!compose.exists()) return DeployResult(false, "Compose file not found")
    return execute("docker", listOf("compose", "-f", compose.absolutePath, "down", "--volumes", "--remove-orphans"))
  }

  override fun apply(): DeployResult = up()

  override fun status(): StatusResult {
    val output = executeCommand("docker", listOf("compose", "-f", composeFile().absolutePath, "ps", "--format", "json"))
    val pods = output.lines().count { it.isNotBlank() }
    return StatusResult(ready = pods > 0, pods = pods, details = output)
  }

  override fun logs(tail: Int): String =
    executeCommand("docker", listOf("compose", "-f", composeFile().absolutePath, "logs", "--tail=$tail"))

  override fun close() = Unit

  private fun execute(
    executable: String,
    args: List<String>,
  ): DeployResult =
    try {
      DeployResult(success = true, message = executeCommand(executable, args))
    } catch (e: Exception) {
      DeployResult(success = false, message = e.message ?: "Unknown error")
    }

  private fun executeCommand(
    executable: String,
    args: List<String>,
  ): String {
    val output = ByteArrayOutputStream()
    val process = ProcessBuilder(listOf(executable) + args).redirectErrorStream(true).start()
    process.inputStream.copyTo(output)
    val exitCode = process.waitFor()
    val text = output.toString(Charsets.UTF_8)
    check(exitCode == 0) { "$executable failed with exit code $exitCode:\n${text.take(1000)}" }
    return text
  }
}
