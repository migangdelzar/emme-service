package com.emme.buildlogic.security

import com.emme.buildlogic.environment.EnvironmentExtension
import com.emme.buildlogic.environment.EnvironmentPlugin
import com.emme.buildlogic.security.provider.GrypeProvider
import com.emme.buildlogic.security.provider.SecurityScannerProvider
import com.emme.buildlogic.security.provider.TrivyProvider
import com.emme.buildlogic.security.task.SecurityScanTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

class SecurityPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    with(project) {
      pluginManager.apply(EnvironmentPlugin::class.java)
      val environment = extensions.getByType(EnvironmentExtension::class.java)
      val extension = extensions.create("emmeSecurity", SecurityExtension::class.java)
      extension.scanner.convention(
        environment
          .value("security.scanner", "trivy", "emme.security.scanner")
          .map(SecurityScanner::fromString),
      )
      extension.severity.convention(
        environment.value("security.severity", "HIGH,CRITICAL", "emme.security.severity"),
      )
      extension.failOnCritical.convention(true)

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
        imageName.set(
          environment.value(
            "container.image.name",
            "emme-service:latest",
            "emme.container.imageName",
            "emme.container.image.name",
          ),
        )
        severity.set(extension.severity)
        reportFile.set(layout.buildDirectory.file("reports/security/container-scan.sarif"))
        scanner.set(securityScanner)
      }
    }
  }
}
