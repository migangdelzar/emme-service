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
  implementation(libs.jackson.databind)
}

tasks.register<JavaExec>("e2eProvision") {
  group = "e2e"
  description = "Provision E2E tenants via platform API"
  classpath = sourceSets["main"].runtimeClasspath
  mainClass = "com.emme.e2eprovisioner.E2eProvisionerApplication"
  environment("E2E_ADMIN_PASSWORD", System.getenv().getOrDefault("E2E_ADMIN_PASSWORD", "E2e-Platform-Admin-2026!"))
}
