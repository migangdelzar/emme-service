package com.emme.buildlogic.plugin

import com.emme.buildlogic.extension.EmmeSecurityExtension
import com.emme.buildlogic.provider.security.TrivyProvider
import com.emme.buildlogic.task.security.SecurityScanTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

class EmmeSecurityPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    with(project) {
      val extension = extensions.create("emmeSecurity", EmmeSecurityExtension::class.java)

      val scannerName = extension.scanner.get()
      val scannerClass =
        when (scannerName.lowercase()) {
          "trivy", "grype" -> TrivyProvider::class.java
          else -> TrivyProvider::class.java
        }

      val securityScanner =
        gradle.sharedServices.registerIfAbsent(
          "emmeSecurityScanner",
          scannerClass,
        ) {
          parameters.scanner.set(extension.scanner)
          parameters.severity.set(extension.severity)
          maxParallelUsages.set(1)
        }

      tasks.register("securityScan", SecurityScanTask::class.java) {
        group = "security"
        description = "Scan container image for vulnerabilities"
        imageName.set(providers.gradleProperty("emme.container.imageName").orElse("emme-studio:latest"))
        severity.set(extension.severity)
        reportFile.set(layout.buildDirectory.file("reports/security/container-scan.sarif"))
        scanner.set(securityScanner)
      }
    }
  }
}
