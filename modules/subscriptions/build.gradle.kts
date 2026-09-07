plugins {
  id("emme.spring-module")
  id("emme.messaging")
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
  implementation(libs.springdoc.openapi.starter.webmvc.ui)
  testImplementation(testFixtures(project(":modules:tenancy")))
  add("integrationTestRuntimeOnly", project(":modules:ai-platform"))
  add("integrationTestImplementation", project(":modules:shared"))
  add("integrationTestImplementation", project(":libraries:kernel"))
  add("integrationTestImplementation", project(":database"))
  add("integrationTestImplementation", testFixtures(project(":modules:tenancy")))
  add("integrationTestImplementation", testFixtures(project(":libraries:testing")))
  add("integrationTestImplementation", libs.spring.boot.starter.data.jpa)
  add("integrationTestImplementation", libs.spring.jdbc)
}
