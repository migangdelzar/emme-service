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
  implementation(project(":modules:appointments"))
  implementation(project(":modules:clients"))
  implementation(project(":modules:identity"))

  implementation(libs.spring.boot.starter.web)
  implementation(libs.spring.boot.starter.validation)
  implementation(libs.spring.boot.starter.oauth2.client)
  implementation(libs.spring.boot.starter.data.redis)
  implementation(libs.springdoc.openapi.starter.webmvc.ui)
  implementation(libs.okhttp)
  implementation(libs.jackson.databind)
  testImplementation(testFixtures(project(":modules:tenancy")))
  testImplementation(libs.spring.boot.webmvc.test)
  testImplementation(libs.spring.security.test)
  add("integrationTestImplementation", testFixtures(project(":modules:tenancy")))
  add("integrationTestImplementation", testFixtures(project(":libraries:testing")))
  add("integrationTestImplementation", libs.okhttp)
  add("integrationTestImplementation", libs.jackson.databind)
}
