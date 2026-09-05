plugins {
  id("emme.test-fixtures")
}

group = "com.emme"

dependencies {
implementation(platform(project(":platform")))
  testFixturesImplementation(platform(project(":platform")))
  testFixturesImplementation(libs.spring.boot.starter.test)
  testFixturesImplementation(libs.spring.boot.starter.web)
  testFixturesImplementation(libs.spring.boot.webmvc.test)
  testFixturesImplementation(libs.spring.boot.starter.security)
  testFixturesImplementation(libs.spring.boot.starter.oauth2.resource.server)
  testFixturesImplementation(libs.spring.boot.starter.oauth2.client)
  testFixturesImplementation(libs.spring.boot.starter.data.jpa)
  testFixturesImplementation("org.springframework.boot:spring-boot-autoconfigure")
  testFixturesImplementation(libs.spring.boot.starter.data.redis)
  testFixturesImplementation(libs.spring.security.test)
  testFixturesImplementation(libs.archunit.junit5)
  testFixturesImplementation(libs.jackson.databind)
  testFixturesImplementation(libs.okhttp)
  testFixturesImplementation(libs.spring.modulith.starter.jpa)

}
