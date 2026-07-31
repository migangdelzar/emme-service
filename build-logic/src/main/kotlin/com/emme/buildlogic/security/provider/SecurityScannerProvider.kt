package com.emme.buildlogic.security.provider

import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.io.File

abstract class SecurityScannerProvider : BuildService<SecurityScannerProvider.Params> {
  interface Params : BuildServiceParameters {
    val scanner: Property<String>
    val severity: Property<String>
  }

  abstract fun scan(
    image: String,
    output: File,
  ): SecurityScanResult
}
