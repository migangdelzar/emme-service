plugins {
    base
    id("com.emme.root")
    id("emme.secrets")
    alias(libs.plugins.spotless)
    alias(libs.plugins.owasp.dependency.check)
}

group = "com.emme"
version = "2026.07.0"

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    format("root") {
        target("*.md", "*.kts", "*.yaml", "*.yml", ".gitignore")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

configure<org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension> {
    formats.set(listOf("HTML", "JSON", "SARIF"))
    failBuildOnCVSS.set(7.0f)
  failOnError.set(true)
  skipTestGroups.set(true)
  outputDirectory.set(layout.buildDirectory.dir("reports/dependency-check"))
  nvd.apiKey.set(providers.environmentVariable("NVD_API_KEY"))
  nvd.validForHours.set(24)
}

tasks.named("check") {
    dependsOn("spotlessCheck")
}

tasks.register("coverageCheck") {
    group = "verification"
    description = "Run JaCoCo reporting and coverage verification for emme-platform"
    dependsOn(
        ":applications:emme-platform:test",
        ":applications:emme-platform:jacocoTestReport",
        ":applications:emme-platform:jacocoTestCoverageVerification",
    )
}

gradle.projectsEvaluated {
    tasks.named("ci") {
        dependsOn(subprojects.flatMap { subproject ->
            subproject.tasks.matching { task -> task.name == "check" }
        })
    }
}
