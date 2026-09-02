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
  implementation(libs.spring.ai.model)
  implementation(libs.spring.ai.client.chat)
  implementation(libs.spring.ai.tool.search.advisor)

  testImplementation(libs.spring.boot.starter.test)
}
