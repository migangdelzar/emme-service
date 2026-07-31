import com.emme.buildlogic.core.dependency.EmmeDependencies
import org.gradle.api.artifacts.VersionCatalogsExtension

val libs = extensions.getByType(VersionCatalogsExtension::class).named("libs")
val e = EmmeDependencies(libs)

plugins {
  id("emme.java-base")
  id("org.springframework.boot")
  id("emme.modulith")
  id("emme.testing")
}

dependencies {
  implementation(platform(project(":platform")))
  developmentOnly(platform(project(":platform")))

  implementation(e.springBootStarter)
  implementation(e.springBootStarterWeb)
  implementation(e.springBootStarterValidation)
  implementation(e.springBootStarterSecurity)
  implementation(e.springBootStarterOauth2ResourceServer)

  implementation(e.springBootStarterDataJpa)
  implementation(e.springBootStarterDataRedis)
  implementation(e.springBootStarterActuator)

  implementation(e.liquibaseCore)

  implementation(e.springModulithStarterCore)
  implementation(e.springModulithStarterJpa)

  runtimeOnly(e.springModulithObservability)

  testImplementation(platform(project(":platform")))
  testImplementation(e.springBootStarterTest)
  testImplementation(e.springModulithStarterTest)
}

tasks.named<Jar>("jar") {
  enabled = false
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
  archiveFileName.set("emme-studio.jar")
}
