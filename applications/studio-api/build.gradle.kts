plugins {
  id("emme.spring-application")
  id("emme.modulith")
  id("emme.integration-testing")
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

val e2eTestImplementation by configurations.getting {
  extendsFrom(configurations.testImplementation.get())
}
configurations["e2eTestRuntimeOnly"].extendsFrom(configurations.testRuntimeOnly.get())

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

  // Libraries
  implementation(project(":libraries:kernel"))

  // Infrastructure
  runtimeOnly(libs.postgresql)

  // Database migrations — Liquibase changelogs
  runtimeOnly(project(":database"))

  // DevTools — hot reload during local development
  developmentOnly("org.springframework.boot:spring-boot-devtools")

  // Jackson — required by Google OAuth and other modules
  implementation("com.fasterxml.jackson.core:jackson-databind")
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
  "e2eTestImplementation"(libs.assertj.core)
  "e2eTestImplementation"(libs.okhttp)
  "e2eTestImplementation"(libs.okhttp.logging.interceptor)
  "e2eTestRuntimeOnly"(libs.junit.platform.launcher)

  // Integration tests — cross-module smoke tests
  add("integrationTestImplementation", testFixtures(project(":modules:tenancy")))
  add("integrationTestImplementation", testFixtures(project(":libraries:testing")))
}

// ── bootRun ──
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
  systemProperty(
    "spring.profiles.active",
    providers.gradleProperty("emme.profile").orElse("local").get(),
  )
  jvmArgs("--enable-preview")
}

// ── E2E Test Task ──
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

// ── Extensions ──
emmeContainer {
  enabled.set(true)
  imageName.set("ghcr.io/migangdelzar/emme-service-studio-api")
  contextDirectory.set(layout.projectDirectory)
}
emmePublishing {
  enabled.set(true)
  registry.set("ghcr.io/migangdelzar")
  signArtifacts.set(true)
}
emmeDeployment {
  deploymentDir.set(rootProject.layout.projectDirectory.dir("deployment"))
}
