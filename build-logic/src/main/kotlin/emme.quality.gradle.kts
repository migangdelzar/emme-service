plugins {
  id("com.diffplug.spotless")
  id("com.autonomousapps.dependency-analysis")
  jacoco
  id("org.sonarqube")
}

spotless {
  java {
    googleJavaFormat()
    removeUnusedImports()
    trimTrailingWhitespace()
    endWithNewline()
  }
}

tasks.named("check") {
  dependsOn("spotlessCheck")
}

// SonarQube properties — configure via -Psonar.host.url=...
sonar {
  properties {
    property("sonar.projectKey", "emme")
    property("sonar.organization", "emme-nails")
    property(
      "sonar.host.url",
      providers
        .gradleProperty("sonar.host.url")
        .orElse(providers.environmentVariable("SONAR_HOST_URL"))
        .orElse(""),
    )
    property(
      "sonar.coverage.jacoco.xmlReportPaths",
      "${layout.buildDirectory.get().asFile}/reports/jacoco/test/jacocoTestReport.xml",
    )
  }
}

tasks.withType<JacocoReport>().configureEach {
  reports {
    xml.required.set(true)
    html.required.set(true)
    csv.required.set(false)
  }
}

tasks.withType<JacocoCoverageVerification>().configureEach {
  violationRules {
    rule {
      limit {
        minimum = BigDecimal(0.70)
      }
    }
  }
}

// Apply OWASP dependency check if plugin is available
pluginManager.withPlugin("org.owasp.dependencycheck") {
  configure<org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension> {
    formats.set(listOf("HTML", "JSON"))
    failBuildOnCVSS.set(7.0f)
    failOnError.set(true)
    skipTestGroups.set(true)
  }
}

tasks.register("jacocoAggregateReport", JacocoReport::class.java) {
  group = "verification"
  description = "Aggregate JaCoCo coverage across all modules"

  // Reports from all subprojects with JaCoCo
  subprojects.forEach { subproject ->
    subproject.pluginManager.withPlugin("jacoco") {
      val reportTask = subproject.tasks.named("jacocoTestReport")
      executionData(reportTask.map { (it as JacocoReport).executionData })
    }
  }
}
