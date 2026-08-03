package com.emme.buildlogic

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

class NativeImageConventionFunctionalTest {
  @Test
  fun `registers native image tasks only when the capability is applied`(
    @TempDir projectDir: Path,
  ) {
    writeSettings(projectDir)
    projectDir.resolve("build.gradle.kts").writeText(
      """
      plugins {
          id("emme.java-base")
          id("emme.native-image")
      }
      """.trimIndent(),
    )

    val result =
      GradleRunner
        .create()
        .withProjectDir(projectDir.toFile())
        .withArguments("tasks", "--all", "--stacktrace")
        .build()

    assertThat(result.output).contains("nativeCompile")
    assertThat(result.output).contains("nativeTest")
  }

  @Test
  fun `does not add native image tasks to the default Java convention`(
    @TempDir projectDir: Path,
  ) {
    writeSettings(projectDir)
    projectDir.resolve("build.gradle.kts").writeText(
      """
      plugins {
          id("emme.java-base")
      }
      """.trimIndent(),
    )

    val result =
      GradleRunner
        .create()
        .withProjectDir(projectDir.toFile())
        .withArguments("tasks", "--all", "--stacktrace")
        .build()

    assertThat(result.output).doesNotContain("nativeCompile")
  }

  private fun writeSettings(projectDir: Path) {
    val buildLogicPath = findBuildLogicDir()
    projectDir.resolve("settings.gradle.kts").writeText(
      """
      pluginManagement {
          includeBuild("${escapePath(buildLogicPath)}")
          repositories {
              gradlePluginPortal()
              mavenCentral()
          }
      }
      ${dependencyRepositories()}
      rootProject.name = "test-native-image"
      """.trimIndent(),
    )
  }
}
