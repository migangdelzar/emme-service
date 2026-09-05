plugins {
  id("emme.java-library")
  id("emme.integration-testing")
  id("emme.persistence")
  id("emme.modulith")
  `java-test-fixtures`
}

dependencies {
  api(project(":libraries:functional"))
  implementation(project(":libraries:kernel"))
  implementation(libs.spring.context)
  implementation(libs.spring.jdbc)
  implementation(libs.spring.web)
  implementation(libs.spring.webmvc)
  implementation(libs.spring.security.core)
  implementation(libs.java.uuid.generator)

  testImplementation(libs.spring.boot.starter.data.redis)
  testImplementation(libs.spring.boot.starter.security)
  testImplementation(libs.spring.boot.starter.oauth2.resource.server)
  testImplementation(libs.spring.security.test)
  testImplementation(libs.jackson.databind)

  // Test fixtures — shared base classes consumed by all modules
  testFixturesImplementation(platform(project(":platform")))
  testFixturesImplementation(libs.spring.boot.starter.test)
  testFixturesImplementation(libs.spring.boot.webmvc.test)
  testFixturesImplementation(libs.spring.boot.data.jpa.test)
  testFixturesImplementation(libs.spring.boot.starter.security)
  testFixturesImplementation(libs.spring.boot.starter.oauth2.resource.server)
  testFixturesImplementation(libs.spring.security.test)
  testFixturesImplementation(libs.testcontainers.postgresql)
  testFixturesImplementation(libs.testcontainers.junit.jupiter)
  testFixturesImplementation(libs.jackson.databind)
  testFixturesImplementation("org.springframework.boot:spring-boot-testcontainers")

  // Integration test deps — shared int tests need tenancy + studio entities
  add("integrationTestImplementation", testFixtures(project(":modules:tenancy")))
  add("integrationTestImplementation", testFixtures(project(":libraries:testing")))
  add("integrationTestImplementation", libs.spring.jdbc)
}
