plugins {
  id("emme.spring-module")
  id("emme.integration-testing")
  id("emme.spring-web")
  id("emme.persistence")
}

dependencies {
  implementation(project(":modules:shared"))
  implementation(project(":libraries:kernel"))
  implementation(project(":modules:tenancy"))

  implementation(libs.spring.boot.starter.web)
  implementation(libs.spring.boot.starter.validation)
  implementation(libs.spring.boot.starter.security)
  implementation(libs.springdoc.openapi.starter.webmvc.ui)
  implementation(libs.okhttp)
  implementation(libs.jackson.databind)
  testImplementation(testFixtures(project(":libraries:testing")))
  testImplementation(libs.spring.boot.webmvc.test)
  testImplementation(libs.spring.security.test)
  testImplementation(project(":modules:studio"))
  testImplementation(project(":modules:identity"))
  add("integrationTestImplementation", testFixtures(project(":modules:tenancy")))
  add("integrationTestImplementation", testFixtures(project(":libraries:testing")))
}
