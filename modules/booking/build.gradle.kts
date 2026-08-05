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
  implementation(project(":libraries:kernel"))
  implementation(project(":modules:clients"))
  implementation(project(":libraries:kernel"))
  implementation(project(":modules:staffing"))
  implementation(project(":libraries:kernel"))
  implementation(project(":modules:catalog"))
  implementation(project(":libraries:kernel"))
  testImplementation(testFixtures(project(":libraries:testing")))
  add("integrationTestImplementation", testFixtures(project(":modules:tenancy")))
  add("integrationTestImplementation", testFixtures(project(":libraries:testing")))
}
