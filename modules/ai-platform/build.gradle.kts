plugins {
  id("emme.spring-module")
}

group = "com.emme"

dependencies {
  api(project(":libraries:ai-contracts"))
  implementation(libs.spring.boot.starter)
  implementation(libs.spring.jdbc)
  implementation(libs.jackson.databind)
  implementation(libs.okhttp)

  testImplementation(libs.spring.boot.starter.test)
}
