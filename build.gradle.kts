plugins {
    base
    id("com.emme.root")
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
}

tasks.named("check") {
    dependsOn("spotlessCheck")
}

gradle.projectsEvaluated {
    tasks.named("ci") {
        dependsOn(subprojects.flatMap { subproject ->
            subproject.tasks.matching { task -> task.name == "check" }
        })
    }
}
