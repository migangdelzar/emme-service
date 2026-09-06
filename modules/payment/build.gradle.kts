plugins {
  id("emme.spring-module")
  id("emme.integration-testing")
  id("emme.spring-web")
  id("emme.persistence")
}

dependencies {
  implementation(project(":libraries:ai-contracts"))
  implementation(project(":modules:shared"))
  implementation(project(":libraries:kernel"))
  implementation(project(":modules:tenancy"))

  implementation(libs.spring.boot.starter.web)
  implementation(libs.spring.boot.restclient)
  implementation(libs.spring.boot.starter.validation)
  implementation(libs.jackson.databind)
  testImplementation(testFixtures(project(":modules:tenancy")))
  testImplementation(libs.spring.boot.webmvc.test)
  testImplementation(libs.okhttp.mockwebserver)

  testRuntimeOnly(libs.h2)
  add("integrationTestImplementation", testFixtures(project(":modules:tenancy")))
  add("integrationTestImplementation", testFixtures(project(":libraries:testing")))
  add("integrationTestImplementation", libs.spring.boot.restclient)
  add("integrationTestImplementation", libs.jackson.databind)
  add("integrationTestImplementation", libs.okhttp.mockwebserver)
}
