package com.emme.buildlogic

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

class ConfigurationCacheFunctionalTest {
  @Test
  fun `reuses configuration cache for an unchanged capability build`(
    @TempDir projectDir: Path,
  ) {
    writeSettings(projectDir)
    projectDir.resolve("build.gradle.kts").writeText(
      """
      plugins {
          id("emme.java-library")
      }
      """.trimIndent(),
    )

    val first = run(projectDir)
    val second = run(projectDir)

    assertThat(first.output).contains("Configuration cache entry stored")
    assertThat(second.output).contains("Reusing configuration cache")
  }

  private fun run(projectDir: Path) =
    GradleRunner
      .create()
      .withProjectDir(projectDir.toFile())
      .withArguments("tasks", "--configuration-cache", "--stacktrace")
      .build()

  private fun writeSettings(projectDir: Path) {
    projectDir.resolve("settings.gradle.kts").writeText(
      """
      pluginManagement {
          includeBuild("${escapePath(findBuildLogicDir())}")
          repositories {
              gradlePluginPortal()
              mavenCentral()
          }
      }
      dependencyResolutionManagement {
          repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
          repositories { mavenCentral() }
          versionCatalogs {
              create("libs") {
                  from(files("${escapePath(findCatalogPath())}"))
              }
          }
      }
      rootProject.name = "test-configuration-cache"
      include(":platform", ":libraries:testing", ":libraries:functional")
      """.trimIndent(),
    )

    projectDir.resolve("platform").toFile().mkdirs()
    projectDir.resolve("platform/build.gradle.kts").writeText(
      """
      plugins { `java-platform` }
      javaPlatform { allowDependencies() }
      """.trimIndent(),
    )
    writeTestFixtureProject(projectDir)
    writeFunctionalProject(projectDir)
  }
}
