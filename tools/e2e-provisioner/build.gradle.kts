plugins {
  id("emme.java-library")
  application
}

group = "com.emme.tools"

application {
  mainClass = "com.emme.e2eprovisioner.E2eProvisionerApplication"
}

dependencies {
  implementation(platform(project(":platform")))
  implementation(project(":modules:shared"))
  implementation(libs.jackson.databind)
  implementation(libs.postgresql)
  implementation(libs.spring.jdbc)
  testImplementation(platform(project(":platform")))
  testImplementation(libs.assertj.core)
  testImplementation(libs.junit.jupiter)
  testRuntimeOnly(libs.junit.platform.launcher)
}
