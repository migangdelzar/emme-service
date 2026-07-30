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
  implementation(project(":modules:tenancy"))
  implementation(project(":libraries:kernel"))
  implementation(project(":modules:studio"))
  implementation(project(":libraries:kernel"))
  implementation(project(":modules:customer"))
  implementation(project(":libraries:kernel"))
  implementation(project(":modules:workforce"))
  implementation(project(":libraries:kernel"))
  implementation(project(":modules:catalog"))
  implementation(project(":libraries:kernel"))
  testImplementation(testFixtures(project(":libraries:testing")))
  add("integrationTestImplementation", testFixtures(project(":modules:tenancy")))
  add("integrationTestImplementation", testFixtures(project(":libraries:testing")))
}
