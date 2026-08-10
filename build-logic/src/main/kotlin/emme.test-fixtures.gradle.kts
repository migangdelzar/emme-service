import com.emme.buildlogic.core.dependency.Dependencies
import org.gradle.api.artifacts.VersionCatalogsExtension

val libs = extensions.getByType(VersionCatalogsExtension::class).named("libs")
val e = Dependencies(libs)

plugins {
  id("emme.java-library")
  `java-test-fixtures`
}

dependencies {
  testFixturesImplementation(platform(project(":platform")))
  testFixturesImplementation(e.assertjCore)
}
