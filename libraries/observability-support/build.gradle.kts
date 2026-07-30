plugins {
  id("emme.java-library")
}

dependencies {
  implementation(libs.micrometer.tracing.bridge.otel)
  implementation(libs.micrometer.registry.prometheus)
}
