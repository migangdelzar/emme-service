package com.emme.buildlogic.task.publishing

import com.emme.buildlogic.provider.publishing.PublisherProvider
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class SignArtifactsTask : DefaultTask() {

    @get:InputFile
    abstract val artifact: RegularFileProperty

    @get:Input
    abstract val keyId: Property<String>

    @get:OutputFile
    abstract val signatureFile: RegularFileProperty

    @get:Internal
    abstract val publisher: Property<PublisherProvider>

    @TaskAction
    fun sign() {
        val result = publisher.get().sign(artifact.get().asFile, keyId.get())
        signatureFile.get().asFile.writeText(result.signaturePath)
        logger.lifecycle("Artifact signed: {}", result.signaturePath)
    }
}
