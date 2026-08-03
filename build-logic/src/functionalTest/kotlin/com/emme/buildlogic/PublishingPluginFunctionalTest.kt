package com.emme.buildlogic

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

class PublishingPluginFunctionalTest {
  @Test
  fun `registers publishing tasks without executing git or publishing work`(
    @TempDir projectDir: Path,
  ) {
    writeSettings(projectDir)
    projectDir.resolve("build.gradle.kts").writeText(
      """
      plugins {
          id("emme.publishing")
      }
      """.trimIndent(),
    )

    val result =
      GradleRunner
        .create()
        .withProjectDir(projectDir.toFile())
        .withArguments("tasks", "--group=publishing", "--stacktrace")
        .build()

    assertThat(result.output).contains("publishBuildInfo", "publishVerifyVersion", "BUILD SUCCESSFUL")
  }

  @Test
  fun `enabled publishing generates build metadata lazily`(
    @TempDir projectDir: Path,
  ) {
    writeSettings(projectDir)
    projectDir.resolve("build.gradle.kts").writeText(
      """
      plugins {
          id("emme.publishing")
      }

      version = "1.2.3"

      emmePublishing {
          enabled.set(true)
      }
      """.trimIndent(),
    )

    val result =
      GradleRunner
        .create()
        .withProjectDir(projectDir.toFile())
        .withArguments("publishBuildInfo", "--stacktrace")
        .build()

    val metadata = projectDir.resolve("build/publishing/build-info.properties")
    assertThat(result.output).contains("BUILD SUCCESSFUL")
    assertThat(metadata).exists()
    assertThat(metadata.readText()).contains(
      "build.version=1.2.3",
      "build.branch=unknown",
      "build.commit=unknown",
    )
  }

  @Test
  fun `invalid release version fails the verification task`(
    @TempDir projectDir: Path,
  ) {
    writeSettings(projectDir)
    projectDir.resolve("build.gradle.kts").writeText(
      """
      plugins {
          id("emme.publishing")
      }

      version = "not-a-version"

      emmePublishing {
          enabled.set(true)
      }
      """.trimIndent(),
    )

    val result =
      GradleRunner
        .create()
        .withProjectDir(projectDir.toFile())
        .withArguments("publishVerifyVersion", "--stacktrace")
        .buildAndFail()

    assertThat(result.output).contains("does not match semantic versioning format")
  }

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
      ${dependencyRepositories()}
      rootProject.name = "test-publishing"
      """.trimIndent(),
    )
  }
}
