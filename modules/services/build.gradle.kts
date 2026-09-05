plugins {
  id("emme.spring-module")
  id("emme.integration-testing")
  id("emme.spring-web")
  id("emme.persistence")
  id("emme.testing")
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
  testImplementation(testFixtures(project(":modules:tenancy")))
}
