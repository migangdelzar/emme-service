package com.emme.buildlogic.task.publishing

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.ByteArrayOutputStream

abstract class GenerateSbomTask : DefaultTask() {

    @get:OutputFile
    abstract val sbomFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val output = sbomFile.get().asFile
        output.parentFile.mkdirs()

        // CycloneDX with --output-file writes exactly the declared @OutputFile,
        // keeping incremental UP-TO-DATE checks accurate
        val process = ProcessBuilder(
            "cyclonedx", "gradle", "app", "--include",
            "--output-file", output.absolutePath,
            "--output-format", "json",
        ).redirectErrorStream(true).start()

        val execOutput = ByteArrayOutputStream()
        process.inputStream.copyTo(execOutput)
        val exitCode = process.waitFor()
        val text = execOutput.toString(Charsets.UTF_8)

        check(exitCode == 0) { "CycloneDX SBOM generation failed:\n${text.take(1000)}" }
        logger.lifecycle("SBOM generated: {}", output.absolutePath)
    }
}
