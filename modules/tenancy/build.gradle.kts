plugins {
  id("emme.spring-module")
  id("emme.spring-web")
  id("emme.persistence")
  id("emme.integration-testing")
  `java-test-fixtures`
}

dependencies {
  implementation(project(":modules:shared"))
  implementation(project(":libraries:kernel"))

  implementation(libs.spring.boot.starter.web)
  implementation(libs.spring.boot.starter.security)
  implementation(libs.spring.boot.starter.validation)
  implementation(libs.spring.boot.starter.data.redis)
  implementation(libs.spring.boot.starter.oauth2.resource.server)
  implementation(libs.liquibase.core)
  implementation(libs.springdoc.openapi.starter.webmvc.ui)
  implementation(libs.caffeine)
  implementation(libs.shedlock.spring)
  implementation(libs.shedlock.provider.jdbc.template)

  // Unit / slice tests (H2, fast)
  testImplementation(testFixtures(project(":libraries:testing")))
  testImplementation(libs.spring.boot.webmvc.test)
  testImplementation(libs.spring.boot.resttestclient)
  testImplementation(libs.spring.security.test)
  testImplementation(project(":modules:identity"))

  // Test fixtures — tenant-aware helpers consumed by every module
  testFixturesImplementation(platform(project(":platform")))
  testFixturesImplementation(project(":libraries:functional"))
  testFixturesImplementation(project(":libraries:kernel"))
  testFixturesCompileOnly(project(":libraries:test-containers"))
  testFixturesImplementation(libs.spring.boot.starter.test)
  testFixturesImplementation("org.springframework.boot:spring-boot-testcontainers")
  testFixturesImplementation(libs.testcontainers.junit.jupiter)

  // Integration tests: dep on own testFixtures + TestApplication
  add("integrationTestImplementation", testFixtures(project(":modules:tenancy")))
  add("integrationTestImplementation", testFixtures(project(":libraries:testing")))
}
