package com.emme.buildlogic

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

class SecretsPluginFunctionalTest {
  @Test
  fun `registers provider agnostic secret tasks and dry run is safe`(
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
      rootProject.name = "test-secrets"
      """.trimIndent(),
    )
    projectDir.resolve("build.gradle.kts").writeText(
      """
      plugins {
          id("emme.secrets")
      }
      """.trimIndent(),
    )

    val result =
      GradleRunner
        .create()
        .withProjectDir(projectDir.toFile())
        .withArguments("rotateSecrets", "--stacktrace")
        .build()

    assertThat(result.output).contains("No secret rotation declarations", "BUILD SUCCESSFUL")
  }

  @Test
  fun `command line project properties override environment files and gradle properties`(
    @TempDir projectDir: Path,
  ) {
    projectDir.resolve("gradle/environments").toFile().mkdirs()
    projectDir.resolve("gradle/environments/dev.properties").writeText("image.tag=from-file\n")
    projectDir.resolve("gradle.properties").writeText("image.tag=from-gradle\n")
    projectDir.resolve("settings.gradle.kts").writeText(
      """
      pluginManagement {
          includeBuild("${escapePath(findBuildLogicDir())}")
          includeBuild("${escapePath(findBuildLogicSettingsDir())}")
          repositories {
              gradlePluginPortal()
              mavenCentral()
          }
      }
      plugins {
          id("com.emme.environment-settings")
      }
      rootProject.name = "test-environment-precedence"
      """.trimIndent(),
    )
    projectDir.resolve("build.gradle.kts").writeText(
      """
      plugins {
          id("emme.environment")
      }
      """.trimIndent(),
    )

    val result =
      GradleRunner
        .create()
        .withProjectDir(projectDir.toFile())
        .withArguments("environmentReport", "-Penvironment=dev", "-Pimage.tag=from-cli")
        .build()

    assertThat(result.output).contains("Image: local/from-cli", "BUILD SUCCESSFUL")
  }
}
