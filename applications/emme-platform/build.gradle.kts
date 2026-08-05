plugins {
  id("emme.spring-application")
  id("emme.modulith")
  id("emme.messaging")
  id("emme.integration-testing")
  id("emme.container")
  id("emme.publishing")
  id("emme.deployment")
}

// Native delivery is an explicit application-edge experiment. Keeping the JVM
// path as the default makes ordinary development and CI deterministic while
// still exposing the same application to a reproducible native build command.
if (providers.gradleProperty("emme.native-image").map(String::toBoolean).orElse(false).get()) {
  pluginManager.apply("emme.native-image")
}

group = "com.emme"
version = "0.1.0"

// The shell-free container probe is operational tooling, not application
// behavior; keep it out of the business coverage gate while testing it directly.
tasks.withType<org.gradle.testing.jacoco.tasks.JacocoReport>().configureEach {
  classDirectories.setFrom(
    classDirectories.files.map {
      fileTree(it) {
        exclude("com/emme/ContainerHealthCheck.class")
      }
    },
  )
}

tasks.withType<org.gradle.testing.jacoco.tasks.JacocoCoverageVerification>().configureEach {
  classDirectories.setFrom(
    classDirectories.files.map {
      fileTree(it) {
        exclude("com/emme/ContainerHealthCheck.class")
      }
    },
  )
}

// ── E2E Test SourceSet ──
val e2eTest by sourceSets.creating {
  compileClasspath += sourceSets.main.get().output
  runtimeClasspath += sourceSets.main.get().output
}

dependencies {
  implementation(libs.jackson.databind)

  // Business Modules
  implementation(project(":modules:shared"))
  implementation(project(":modules:tenancy"))
  implementation(project(":modules:identity"))
  implementation(project(":modules:studio"))
  implementation(project(":modules:clients"))
  implementation(project(":modules:staffing"))
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

  add("integrationTestImplementation", libs.testcontainers.kafka)
  add("integrationTestImplementation", libs.spring.kafka)
  add("integrationTestImplementation", libs.spring.modulith.events.kafka)
  add("integrationTestImplementation", libs.spring.boot.starter.oauth2.client)
  add("integrationTestImplementation", project(":modules:studio"))
  add("integrationTestImplementation", project(":modules:tenancy"))
  add("integrationTestRuntimeOnly", libs.h2)
}

sourceSets.named("integrationTest") {
  compileClasspath += sourceSets.main.get().output
  runtimeClasspath += sourceSets.main.get().output
}

tasks.register<Test>("e2eTest") {
  description = "Runs black-box E2E flow tests against deployed environment"
  group = "verification"
  testClassesDirs = e2eTest.output.classesDirs
  classpath = e2eTest.runtimeClasspath
  shouldRunAfter(tasks.matching { it.name == "integrationTest" })
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
