plugins {
  id("emme.spring-module")
  id("emme.messaging")
  id("emme.integration-testing")
  id("emme.spring-web")
  id("emme.persistence")
  id("emme.testing")
  id("emme.test-fixtures")
}

dependencies {
  implementation(project(":modules:shared"))
  implementation(project(":libraries:kernel"))
  implementation(project(":modules:tenancy"))
  implementation(libs.spring.boot.starter.web)
  implementation(libs.spring.boot.starter.validation)
  implementation(libs.springdoc.openapi.starter.webmvc.ui)
  testImplementation(testFixtures(project(":modules:tenancy")))
}
