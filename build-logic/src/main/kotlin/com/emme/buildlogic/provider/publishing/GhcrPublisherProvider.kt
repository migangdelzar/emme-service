package com.emme.buildlogic.provider.publishing

import java.io.ByteArrayOutputStream
import java.io.File

abstract class GhcrPublisherProvider : PublisherProvider() {

    override fun sign(artifact: File, keyId: String): SignResult {
        val sigFile = File("${artifact.absolutePath}.asc")
        executeCommand(listOf(
            "gpg", "--batch", "--yes",
            "--local-user", keyId,
            "--detach-sign", "--armor",
            "--output", sigFile.absolutePath,
            artifact.absolutePath,
        ))
        return SignResult(signaturePath = sigFile.absolutePath)
    }

    override fun publish(artifact: File, registry: String): PublishResult {
        val fullImage = "$registry/$artifact"
        val output = executeCommand(listOf("docker", "push", fullImage))
        val digest = output.lines().lastOrNull { it.contains("digest:") }
            ?.substringAfter("digest:")?.trim() ?: "unknown"
        return PublishResult(url = fullImage, digest = digest)
    }

    private fun executeCommand(args: List<String>): String {
        val output = ByteArrayOutputStream()
        val process = ProcessBuilder(args).redirectErrorStream(true).start()
        process.inputStream.copyTo(output)
        val exitCode = process.waitFor()
        val text = output.toString(Charsets.UTF_8)
        check(exitCode == 0) { "${args.first()} failed with exit code $exitCode:\n${text.take(1000)}" }
        return text
    }
}
