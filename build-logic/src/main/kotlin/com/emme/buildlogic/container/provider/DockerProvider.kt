package com.emme.buildlogic.container.provider

import java.io.ByteArrayOutputStream
import java.io.File

abstract class DockerProvider : ContainerRuntimeProvider() {
  override fun build(
    image: String,
    context: File,
    tags: List<String>,
  ): BuildResult {
    val args = mutableListOf("build")
    tags.forEach { args.addAll(listOf("--tag", "$image:$it")) }
    args.add(context.absolutePath)

    val output = executeCommand(args)
    val imageId =
      output
        .lines()
        .lastOrNull { it.startsWith("Successfully built") }
        ?.substringAfter("Successfully built ")
        ?.trim() ?: "unknown"

    val inspectOutput = executeCommand(listOf("inspect", "--format={{.Id}}", "$image:${tags.first()}"))
    val digest = inspectOutput.trim()

    return BuildResult(imageId = imageId, digest = digest)
  }

  override fun push(
    image: String,
    registry: String,
  ): ContainerPushResult {
    val fullImage = if (registry.isNotBlank()) "$registry/$image" else image
    val output = executeCommand(listOf("push", fullImage))
    val manifest =
      output
        .lines()
        .lastOrNull { it.contains("digest:") }
        ?.substringAfter("digest:")
        ?.trim() ?: "unknown"
    return ContainerPushResult(manifest = manifest)
  }

  override fun scan(
    image: String,
    severity: String,
    output: File,
  ): ScanResult {
    output.parentFile.mkdirs()
    val execOutput =
      executeCommand(
        listOf(
          "run",
          "--rm",
          "-v",
          "/var/run/docker.sock:/var/run/docker.sock",
          "aquasec/trivy",
          "image",
          "--severity",
          severity,
          "--format",
          "sarif",
          "--output",
          "/tmp/scan.sarif",
          image,
        ),
      )
    val vulnCount = execOutput.lines().count { it.contains("VulnerabilityID") }
    output.writeText(execOutput)
    return ScanResult(vulnerabilities = vulnCount, reportPath = output.absolutePath)
  }

  override fun close() = Unit

  private fun executeCommand(args: List<String>): String {
    val output = ByteArrayOutputStream()
    val process =
      ProcessBuilder(listOf(parameters.executable.get()) + args)
        .redirectErrorStream(true)
        .start()
    process.inputStream.copyTo(output)
    val exitCode = process.waitFor()
    val text = output.toString(Charsets.UTF_8)
    check(exitCode == 0) { "${parameters.executable.get()} failed with exit code $exitCode:\n${text.take(1000)}" }
    return text
  }
}
