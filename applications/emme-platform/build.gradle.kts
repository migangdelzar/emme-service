plugins {
  id("emme.spring-application")
  id("emme.modulith")
  id("emme.container")
  id("emme.publishing")
  id("emme.deployment")
}

group = "com.emme"
version = "0.1.0"

// ── E2E Test SourceSet ──
val e2eTest by sourceSets.creating {
  compileClasspath += sourceSets.main.get().output
  runtimeClasspath += sourceSets.main.get().output
}

dependencies {
  // Business Modules
  implementation(project(":modules:shared"))
  implementation(project(":modules:tenancy"))
  implementation(project(":modules:identity"))
  implementation(project(":modules:studio"))
  implementation(project(":modules:customer"))
  implementation(project(":modules:workforce"))
  implementation(project(":modules:catalog"))
  implementation(project(":modules:booking"))
  implementation(project(":modules:calendar"))
  implementation(project(":modules:notification"))
  implementation(project(":modules:payment"))
  implementation(project(":modules:assistant"))
  implementation(project(":modules:audit"))

  // Infrastructure
  runtimeOnly(libs.postgresql)

  // Scheduling
  implementation(libs.shedlock.spring)
  implementation(libs.shedlock.provider.jdbc.template)

  // Liquibase
  implementation(libs.liquibase.core)

  // Testing
  testImplementation(testFixtures(project(":libraries:testing")))
  testImplementation(libs.okhttp.mockwebserver)
  testImplementation(libs.greenmail)
  testImplementation(libs.okhttp)

  // E2E tests
  "e2eTestImplementation"(platform(project(":platform")))
  "e2eTestImplementation"(libs.junit.jupiter)
  "e2eTestRuntimeOnly"(libs.junit.platform.launcher)
  "e2eTestImplementation"(libs.assertj.core)
  "e2eTestImplementation"(libs.okhttp)
  "e2eTestImplementation"(libs.okhttp.logging.interceptor)
}

tasks.register<Test>("e2eTest") {
  description = "Runs black-box E2E flow tests against deployed environment"
  group = "verification"
  testClassesDirs = e2eTest.output.classesDirs
  classpath = e2eTest.runtimeClasspath
  shouldRunAfter(tasks.named("integrationTest"))
  useJUnitPlatform()
  jvmArgs("--enable-preview", "-Djava.net.preferIPv4Stack=true")
  systemProperty(
    "emme.e2e.base-url",
    providers
      .gradleProperty("emme.e2e.base-url")
      .orElse(providers.environmentVariable("EMME_E2E_BASE_URL"))
      .orElse("")
      .get(),
  )
}

emmeContainer {
  enabled.set(true)
  imageName.set("ghcr.io/migangdelzar/emme-service")
  contextDirectory.set(layout.projectDirectory)
}

emmePublishing {
  enabled.set(true)
  registry.set("ghcr.io/migangdelzar")
}

emmeDeployment {
  deploymentDir.set(layout.projectDirectory.dir("deployment"))
}
