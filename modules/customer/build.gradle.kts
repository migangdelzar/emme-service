plugins {
  id("emme.spring-module")
  id("emme.testing")
}
dependencies {
  implementation(libs.spring.webmvc)
  testImplementation(testFixtures(project(":libraries:testing")))
}
