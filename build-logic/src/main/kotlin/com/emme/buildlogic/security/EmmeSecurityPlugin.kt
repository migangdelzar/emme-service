package com.emme.buildlogic.security

import com.emme.buildlogic.security.provider.GrypeProvider
import com.emme.buildlogic.security.provider.SecurityScannerProvider
import com.emme.buildlogic.security.provider.TrivyProvider
import com.emme.buildlogic.security.task.SecurityScanTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

class EmmeSecurityPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    with(project) {
      val extension = extensions.create("emmeSecurity", EmmeSecurityExtension::class.java)

      val trivyScanner =
        gradle.sharedServices.registerIfAbsent(
          "emmeTrivyScanner",
          TrivyProvider::class.java,
        ) {
          parameters.scanner.set("trivy")
          parameters.severity.set(extension.severity)
          maxParallelUsages.set(1)
        }

      val grypeScanner =
        gradle.sharedServices.registerIfAbsent(
          "emmeGrypeScanner",
          GrypeProvider::class.java,
        ) {
          parameters.scanner.set("grype")
          parameters.severity.set(extension.severity)
          maxParallelUsages.set(1)
        }

      val securityScanner: Provider<SecurityScannerProvider> =
        extension.scanner.map { scanner ->
          when (scanner) {
            SecurityScanner.TRIVY -> trivyScanner.get()
            SecurityScanner.GRYPE -> grypeScanner.get()
          }
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
