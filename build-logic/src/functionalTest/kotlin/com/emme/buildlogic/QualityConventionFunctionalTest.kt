package com.emme.buildlogic

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

class QualityConventionFunctionalTest {
  @Test
  fun `quality convention registers formatting and coverage tasks`(
    @TempDir projectDir: Path,
  ) {
    writeSettings(projectDir)
    projectDir.resolve("build.gradle.kts").writeText(
      """
      plugins {
          id("emme.quality")
      }
      """.trimIndent(),
    )

    val result =
      GradleRunner
        .create()
        .withProjectDir(projectDir.toFile())
        .withArguments("tasks", "--all", "--stacktrace")
        .build()

    assertThat(result.output).contains("spotlessCheck", "jacocoAggregateReport", "BUILD SUCCESSFUL")
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
      rootProject.name = "test-quality"
      """.trimIndent(),
    )
  }
}
