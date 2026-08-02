package com.emme.buildlogic.security

import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import javax.inject.Inject

abstract class EmmeSecurityExtension
  @Inject
  constructor(
    providers: ProviderFactory,
  ) {
    /** Security scanner selected by the project or CI environment. */
    abstract val scanner: Property<SecurityScanner>

    /** Minimum severity to report: "LOW,MEDIUM,HIGH,CRITICAL" */
    abstract val severity: Property<String>

    /** Fail build when critical vulnerabilities found */
    abstract val failOnCritical: Property<Boolean>

    init {
      scanner.convention(
        providers
          .gradleProperty("emme.security.scanner")
          .orElse(providers.environmentVariable("EMME_SECURITY_SCANNER"))
          .map(SecurityScanner::fromString)
          .orElse(SecurityScanner.TRIVY),
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
