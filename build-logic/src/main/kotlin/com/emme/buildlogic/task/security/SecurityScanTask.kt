package com.emme.buildlogic.task.security

import com.emme.buildlogic.provider.security.SecurityScannerProvider
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class SecurityScanTask : DefaultTask() {

    @get:Input
    abstract val imageName: Property<String>

    @get:Input
    abstract val severity: Property<String>

    @get:OutputFile
    abstract val reportFile: RegularFileProperty

    @get:Internal
    abstract val scanner: Property<SecurityScannerProvider>

    init {
        severity.convention("HIGH,CRITICAL")
    }

    @TaskAction
    fun scan() {
        val result = scanner.get().scan(imageName.get(), reportFile.get().asFile)
        logger.lifecycle("Security scan complete: {} vulnerabilities ({} critical, {} high)",
            result.vulnerabilities, result.critical, result.high)
        if (!result.isClean()) {
            logger.warn("Security vulnerabilities found! See report: {}", result.reportPath)
        }
    }
}
