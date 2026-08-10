package com.emme.buildlogic.deployment.provider

import com.emme.buildlogic.deployment.KubernetesDeploymentTarget
import java.io.ByteArrayOutputStream
import java.io.File

abstract class KubernetesProvider : DeploymentProvider() {
  private fun overlayDir(): File =
    parameters.deploymentDir
      .dir(
        "../infra/kubernetes/overlays/" +
          KubernetesDeploymentTarget.overlayName(parameters.profile.get(), parameters.runtime.get()),
      ).get()
      .asFile

  override fun up(): DeployResult = apply()

  override fun down(): DeployResult {
    val overlay = overlayDir()
    if (!overlay.exists()) return DeployResult(false, "Overlay not found: ${overlay.absolutePath}")
    return kubectl(listOf("delete", "-k", overlay.absolutePath))
  }

  override fun apply(): DeployResult {
    val overlay = overlayDir()
    if (!overlay.exists()) return DeployResult(false, "Overlay not found: ${overlay.absolutePath}")
    return kubectl(listOf("apply", "-k", overlay.absolutePath))
  }

  override fun status(): StatusResult {
    val ns = parameters.namespace.get()
    val output =
      executeCommand(
        "kubectl",
        listOf("rollout", "status", "deployment/${KubernetesWorkload.DEPLOYMENT_NAME}", "-n", ns, "--timeout=30s"),
      )
    val podsOutput = executeCommand("kubectl", listOf("get", "pods", "-n", ns, "--no-headers"))
    val pods = podsOutput.lines().count { it.isNotBlank() }
    return StatusResult(ready = output.contains("successfully rolled out"), pods = pods, details = output)
  }

  override fun logs(tail: Int): String {
    val ns = parameters.namespace.get()
    return executeCommand("kubectl", listOf("logs", "-l", KubernetesWorkload.POD_SELECTOR, "-n", ns, "--tail=$tail"))
  }

  override fun close() = Unit

  private fun kubectl(args: List<String>): DeployResult =
    try {
      DeployResult(success = true, message = executeCommand("kubectl", args))
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
