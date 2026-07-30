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
  implementation(project(":modules:studio"))

  implementation(libs.spring.boot.starter.web)
  implementation(libs.spring.boot.starter.security)
  implementation(libs.spring.boot.starter.oauth2.resource.server)
  implementation(libs.spring.boot.starter.oauth2.client)
  implementation(libs.spring.boot.starter.validation)
  implementation(libs.springdoc.openapi.starter.webmvc.ui)
  implementation(libs.okhttp)
  implementation(libs.jackson.databind)

  testImplementation(libs.spring.security.test)
  testImplementation(testFixtures(project(":libraries:testing")))
  testImplementation(libs.spring.boot.webmvc.test)
  testImplementation(libs.spring.boot.resttestclient)
  add("integrationTestImplementation", testFixtures(project(":modules:tenancy")))
  add("integrationTestImplementation", testFixtures(project(":libraries:testing")))
}
