// Feature flags convention — adds FF4J or similar
// Enabled via: -Pemme.feature.flags=true or EMME_FEATURE_FLAGS=true

val featureFlags =
  providers
    .gradleProperty("emme.feature.flags")
    .orElse(providers.environmentVariable("EMME_FEATURE_FLAGS"))
    .orElse("false")

if (featureFlags.get().toBoolean()) {
  dependencies.add("implementation", "org.ff4j:ff4j-spring-boot-starter")
}
