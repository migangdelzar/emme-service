import com.emme.buildlogic.core.dependency.EmmeDependencies
import org.gradle.api.artifacts.VersionCatalogsExtension

val libs = extensions.getByType(VersionCatalogsExtension::class).named("libs")
val e = EmmeDependencies(libs)

plugins {
  id("emme.spring-module")
}

dependencies {
  implementation(e.springKafka)
  implementation(e.springModulithEventsKafka)
  testImplementation(e.testcontainersKafka)
}
