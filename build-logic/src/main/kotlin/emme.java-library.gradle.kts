plugins {
  id("emme.java-base")
  `java-library`
  id("emme.testing")
}

dependencies {
  implementation(platform(project(":platform")))
  testImplementation(platform(project(":platform")))
}
