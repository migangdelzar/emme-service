plugins {
  id("emme.spring-module")
  id("emme.integration-testing")
  id("emme.spring-web")
  id("emme.persistence")
  id("emme.lombok")
}

dependencies {
  implementation(project(":modules:shared"))
  implementation(project(":libraries:kernel"))
  implementation(project(":libraries:ai-contracts"))
  implementation(project(":modules:tenancy"))
  implementation(project(":modules:subscriptions"))
  implementation(libs.spring.boot.starter.web)
  implementation(libs.spring.boot.starter.validation)
  implementation(libs.springdoc.openapi.starter.webmvc.ui)
  testImplementation(libs.spring.boot.webmvc.test)
  testImplementation(testFixtures(project(":modules:tenancy")))
  add("integrationTestRuntimeOnly", project(":modules:ai-platform"))
  add("integrationTestImplementation", project(":modules:shared"))
  add("integrationTestImplementation", project(":libraries:kernel"))
  add("integrationTestImplementation", project(":database"))
  add("integrationTestImplementation", libs.spring.boot.starter.data.jpa)
  add("integrationTestImplementation", libs.spring.jdbc)
  add("integrationTestImplementation", testFixtures(project(":modules:tenancy")))
  add("integrationTestImplementation", testFixtures(project(":libraries:testing")))
}
