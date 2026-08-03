package com.emme.buildlogic

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

class JavaBaseConventionFunctionalTest {
  @Test
  fun `applies java-base convention with Spotless`(
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
        .withArguments("spotlessCheck", "--stacktrace")
        .build()

    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  @Test
  fun `configures Java 25 toolchain`(
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
        .withArguments("javaToolchains", "--stacktrace")
        .build()

    assertThat(result.output).contains("BUILD SUCCESSFUL")
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
      rootProject.name = "test-java-base"
      """.trimIndent(),
    )
  }
}
