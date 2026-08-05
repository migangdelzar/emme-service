package com.emme.buildlogic.security

import org.gradle.api.provider.Property

abstract class SecurityExtension {
  /** Security scanner selected by the project or CI environment. */
  abstract val scanner: Property<SecurityScanner>

  /** Minimum severity to report: "LOW,MEDIUM,HIGH,CRITICAL" */
  abstract val severity: Property<String>

  /** Fail build when critical vulnerabilities found */
  abstract val failOnCritical: Property<Boolean>
}
