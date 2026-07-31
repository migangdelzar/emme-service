import com.emme.buildlogic.core.dependency.EmmeDependencies
import org.gradle.api.artifacts.VersionCatalogsExtension

val libs = extensions.getByType(VersionCatalogsExtension::class).named("libs")
val e = EmmeDependencies(libs)

plugins {
  id("emme.java-base")
  id("emme.java-library")
}

dependencies {
  implementation(platform(project(":platform")))

  implementation(e.springContext)
  implementation(e.springModulithStarterCore)
  implementation(e.springModulithEventsCore)

  testImplementation(e.springBootStarterTest)
  testImplementation(e.springModulithStarterTest)
}
