plugins {
  id("emme.spring-module")
  id("emme.integration-testing")
  id("emme.spring-web")
  id("emme.persistence")
  id("emme.test-fixtures")
}

dependencies {
  testFixturesImplementation(libs.spring.boot.starter.test)
  implementation(project(":modules:shared"))
  implementation(project(":libraries:kernel"))
  implementation(project(":libraries:ai-contracts"))
  implementation(project(":modules:tenancy"))
  implementation(project(":modules:appointments"))
  implementation(project(":modules:salon"))
  implementation(project(":modules:subscriptions"))

  implementation(libs.spring.boot.starter.web)
  implementation(libs.spring.boot.restclient)
  implementation(libs.spring.boot.starter.security)
  implementation(libs.spring.boot.starter.oauth2.resource.server)
  implementation(libs.spring.boot.starter.oauth2.client)
  implementation(libs.spring.boot.starter.validation)
  implementation(libs.spring.boot.starter.data.redis)
  implementation(libs.springdoc.openapi.starter.webmvc.ui)
  implementation(libs.jackson.databind)

  testImplementation(libs.spring.security.test)
  testImplementation(testFixtures(project(":modules:tenancy")))
  testImplementation(libs.spring.boot.webmvc.test)
  testImplementation(libs.spring.boot.resttestclient)
  add("integrationTestImplementation", testFixtures(project(":modules:tenancy")))
  add("integrationTestImplementation", testFixtures(project(":libraries:testing")))
  add("integrationTestImplementation", libs.spring.boot.restclient)
  add("integrationTestImplementation", libs.jackson.databind)
  add("integrationTestImplementation", libs.okhttp.mockwebserver)
}
