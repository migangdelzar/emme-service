plugins {
  id("emme.spring-module")
}

group = "com.emme"

dependencies {
  api(project(":libraries:ai-contracts"))
  implementation(libs.spring.jdbc)
  implementation(libs.jackson.databind)
  implementation(libs.spring.ai.model)
  implementation(libs.spring.ai.client.chat)
  implementation(libs.spring.ai.ollama)
  implementation(libs.spring.ai.openai) {
    exclude(group = "io.swagger.core.v3", module = "swagger-annotations")
  }
  implementation(libs.spring.ai.tool.search.advisor)

  testImplementation(libs.okhttp.mockwebserver)
}
