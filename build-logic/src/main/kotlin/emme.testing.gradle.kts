import com.emme.buildlogic.core.dependency.Dependencies
import org.gradle.api.artifacts.VersionCatalogsExtension

val libs = extensions.getByType(VersionCatalogsExtension::class).named("libs")
val e = Dependencies(libs)

plugins {
  `jvm-test-suite`
}

testing {
  suites {
    val test by getting(JvmTestSuite::class) {
      useJUnitJupiter()

      dependencies {
        implementation(
          project.dependencies.platform(
            project.dependencies.project(mapOf("path" to ":platform")),
          ),
        )
        implementation(e.assertjCore)
        implementation(e.mockitoCore)
        implementation(e.mockitoJunitJupiter)
        implementation(e.springBootStarterTest)
        // H2 driver for @DataJpaTest and @SpringBootTest (test profile uses H2)
        runtimeOnly(e.h2)
        // Shared test fixtures — every module gets test base classes, JWT helpers, etc.
        implementation(
          project.dependencies.testFixtures(
            project.dependencies.project(mapOf("path" to ":libraries:testing")),
          ),
        )
      }
    }
  }
}
