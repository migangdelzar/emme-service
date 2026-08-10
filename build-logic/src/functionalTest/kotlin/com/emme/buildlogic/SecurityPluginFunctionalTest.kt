package com.emme.buildlogic

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

class SecurityPluginFunctionalTest {
  @Test
  fun `security task is registered without resolving scanner during configuration`(
    @TempDir projectDir: Path,
  ) {
    writeSettings(projectDir)
    projectDir.resolve("gradle.properties").writeText("emme.security.scanner=invalid")
    projectDir.resolve("build.gradle.kts").writeText(
      """
      plugins {
          id("emme.security")
      }
      """.trimIndent(),
    )

    val result =
      GradleRunner
        .create()
        .withProjectDir(projectDir.toFile())
        .withArguments("tasks", "--group=security", "--stacktrace")
        .build()

    assertThat(result.output).contains("securityScan", "BUILD SUCCESSFUL")
  }

  @Test
  fun `unsupported scanner fails when security scan executes`(
    @TempDir projectDir: Path,
  ) {
    writeSettings(projectDir)
    projectDir.resolve("gradle.properties").writeText("emme.security.scanner=invalid")
    projectDir.resolve("build.gradle.kts").writeText(
      """
      plugins {
          id("emme.security")
      }
      """.trimIndent(),
    )

    val result =
      GradleRunner
        .create()
        .withProjectDir(projectDir.toFile())
        .withArguments("securityScan", "--stacktrace")
        .buildAndFail()

    assertThat(result.output).contains("Unsupported security scanner 'invalid'")
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
      rootProject.name = "test-security"
      """.trimIndent(),
    )
  }
}
