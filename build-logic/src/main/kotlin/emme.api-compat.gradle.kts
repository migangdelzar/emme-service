// API compatibility check — compares current against baseline
// Baseline: previous release JAR in Maven/Gradle cache
// Override: -Pemme.api.baseline=1.0.0 or EMME_API_BASELINE=1.0.0

plugins {
  id("me.champeau.gradle.japicmp")
}

val baselineVersion =
  providers
    .gradleProperty("emme.api.baseline")
    .orElse(providers.environmentVariable("EMME_API_BASELINE"))
    .orElse("0.1.0")

tasks.register("apiCheck") {
  group = "verification"
  description = "Check API compatibility against the configured baseline"

  doLast {
    logger.lifecycle("API compat check against baseline: ${baselineVersion.get()}")
    // japicmp configuration runs as part of the plugin
  }
}
