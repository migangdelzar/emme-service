plugins {
  id("emme.spring-module")
  id("emme.integration-testing")
  id("emme.spring-web")
  id("emme.persistence")
  id("emme.messaging")
}

dependencies {
  implementation(project(":modules:shared"))
  implementation(project(":libraries:kernel"))
  implementation(project(":modules:tenancy"))
  implementation(project(":modules:services"))
  implementation(project(":modules:clients"))
  implementation(project(":modules:salon"))
  implementation(project(":modules:subscriptions"))
  implementation(project(":modules:notification"))
  implementation(libs.spring.boot.starter.web)
  implementation(libs.spring.boot.starter.validation)
  implementation(libs.spring.boot.starter.security)
  implementation(libs.springdoc.openapi.starter.webmvc.ui)
  testImplementation(testFixtures(project(":modules:tenancy")))
  testImplementation(libs.spring.boot.webmvc.test)
  testImplementation(libs.spring.security.test)
}
