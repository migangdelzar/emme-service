plugins {
  id("emme.spring-module")
  id("emme.persistence")
  id("emme.testing")
}

dependencies {
  implementation(project(":modules:shared"))
  implementation(project(":modules:tenancy"))
  testImplementation(testFixtures(project(":libraries:testing")))
}
