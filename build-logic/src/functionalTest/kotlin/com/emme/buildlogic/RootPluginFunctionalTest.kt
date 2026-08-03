package com.emme.buildlogic

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

class RootPluginFunctionalTest {
  @Test
  fun `root plugin registers repository lifecycle tasks`(
    @TempDir projectDir: Path,
  ) {
    projectDir.resolve("settings.gradle.kts").writeText(
      """
      pluginManagement {
          includeBuild("${escapePath(findBuildLogicDir())}")
          repositories {
              gradlePluginPortal()
              mavenCentral()
          }
      }
      rootProject.name = "test-root"
      """.trimIndent(),
    )
    projectDir.resolve("build.gradle.kts").writeText(
      """
      plugins {
          id("com.emme.root")
      }
      """.trimIndent(),
    )

    val result =
      GradleRunner
        .create()
        .withProjectDir(projectDir.toFile())
        .withArguments("tasks", "--all", "--stacktrace")
        .build()

    assertThat(result.output).contains("ci", "full", "integrationTest", "BUILD SUCCESSFUL")
  }
}
