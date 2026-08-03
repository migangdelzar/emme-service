plugins {
  id("emme.spring-module")
  id("emme.testing")
}

dependencies {
  testImplementation(testFixtures(project(":libraries:testing")))
}
