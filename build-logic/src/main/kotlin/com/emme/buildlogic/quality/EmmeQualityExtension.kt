package com.emme.buildlogic.quality

import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import javax.inject.Inject

abstract class EmmeQualityExtension
  @Inject
  constructor(
    providers: ProviderFactory,
  ) {
    /** SonarQube server URL */
    abstract val sonarHostUrl: Property<String>

    /** SonarQube project key */
    abstract val projectKey: Property<String>

    /** Quality gate mode: "strict", "warn", or "report" */
    abstract val gateMode: Property<String>

    /** Coverage threshold (0.0 to 1.0) */
    abstract val coverageThreshold: Property<Double>

    init {
      sonarHostUrl.convention(
        providers
          .gradleProperty("sonar.host.url")
          .orElse(providers.environmentVariable("SONAR_HOST_URL"))
          .orElse(""),
      )
      projectKey.convention("emme")
      gateMode.convention(
        providers
          .gradleProperty("emme.quality.gate")
          .orElse(providers.environmentVariable("EMME_QUALITY_GATE"))
          .orElse("strict"),
      )
      coverageThreshold.convention(0.70)
    }
  }
