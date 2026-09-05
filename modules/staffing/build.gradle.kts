plugins {
  id("emme.spring-module")
  id("emme.testing")
}

dependencies {
  implementation(project(":modules:shared"))
  implementation(project(":modules:tenancy"))
}
