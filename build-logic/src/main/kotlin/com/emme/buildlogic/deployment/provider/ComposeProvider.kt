package com.emme.buildlogic.deployment.provider

import java.io.ByteArrayOutputStream
import java.io.File

abstract class ComposeProvider : DeploymentProvider() {
  private val environmentName: String
    get() =
      parameters.profile.get().let { profile ->
        if (profile == "test") "ci" else profile
      }

  private fun composeFiles(): List<File> {
    val composeDirectory =
      parameters.deploymentDir
        .dir("compose")
        .get()
        .asFile
    return listOf(
      File(composeDirectory, "compose.yaml"),
      File(composeDirectory, "compose.runtime-${parameters.runtime.get()}.yaml"),
      File(composeDirectory, "compose.environment-$environmentName.yaml"),
    ).filter(File::exists)
  }

  private fun envFile(): File =
    parameters.deploymentDir
      .file("compose/env/environment-$environmentName.env")
      .get()
      .asFile

  private fun composeArguments(): MutableList<String> =
    mutableListOf<String>("compose").also { arguments ->
      composeFiles().forEach { file ->
        arguments.addAll(listOf("-f", file.absolutePath))
      }
    }

  override fun up(): DeployResult {
    val compose = composeFiles()
    if (compose.size < 2) return DeployResult(false, "Compose base and runtime files were not found")
    val args = composeArguments()
    val env = envFile()
    if (env.exists()) args.addAll(listOf("--env-file", env.absolutePath))
    args.addAll(listOf("up", "-d"))
    return execute("docker", args)
  }

  override fun down(): DeployResult {
    if (composeFiles().size < 2) return DeployResult(false, "Compose base and runtime files were not found")
    return execute(
      "docker",
      composeArguments().also { it.addAll(listOf("down", "--volumes", "--remove-orphans")) },
    )
  }

  override fun apply(): DeployResult = up()

  override fun status(): StatusResult {
    val output = executeCommand("docker", composeArguments().also { it.addAll(listOf("ps", "--format", "json")) })
    val pods = output.lines().count { it.isNotBlank() }
    return StatusResult(ready = pods > 0, pods = pods, details = output)
  }

  override fun logs(tail: Int): String =
    executeCommand(
      "docker",
      composeArguments().also { it.addAll(listOf("logs", "--tail=$tail")) },
    )

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
