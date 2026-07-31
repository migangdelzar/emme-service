package com.emme.buildlogic.extension

import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import javax.inject.Inject

abstract class EmmeSecurityExtension
  @Inject
  constructor(
    providers: ProviderFactory,
  ) {
    /** Security scanner: "trivy" or "grype" */
    abstract val scanner: Property<String>

    /** Minimum severity to report: "LOW,MEDIUM,HIGH,CRITICAL" */
    abstract val severity: Property<String>

    /** Fail build when critical vulnerabilities found */
    abstract val failOnCritical: Property<Boolean>

    init {
      scanner.convention(
        providers
          .gradleProperty("emme.security.scanner")
          .orElse(providers.environmentVariable("EMME_SECURITY_SCANNER"))
          .orElse("trivy"),
      )
      severity.convention(
        providers
          .gradleProperty("emme.security.severity")
          .orElse(providers.environmentVariable("EMME_SECURITY_SEVERITY"))
          .orElse("HIGH,CRITICAL"),
      )
      failOnCritical.convention(true)
    }
  }
