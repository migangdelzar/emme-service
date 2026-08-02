package com.emme.buildlogic

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

class ConventionCapabilitiesFunctionalTest {
  @Test
  fun `java library convention composes test capability`(
    @TempDir projectDir: Path,
  ) {
    writeFixtureProjects(projectDir)
    writeBuild(projectDir, "emme.java-library")

    val result = runTasks(projectDir)

    assertThat(result.output).contains("test", "testFixturesJar", "BUILD SUCCESSFUL")
  }

  @Test
  fun `testing capability creates the standard test suite`(
    @TempDir projectDir: Path,
  ) {
    writeFixtureProjects(projectDir)
    writeBuild(
      projectDir,
      """
      java
      id("emme.testing")
      """.trimIndent(),
    )

    val result = runTasks(projectDir)

    assertThat(result.output).contains("test", "BUILD SUCCESSFUL")
  }

  @Test
  fun `test fixtures capability creates fixture publication tasks`(
    @TempDir projectDir: Path,
  ) {
    writeFixtureProjects(projectDir)
    writeBuild(projectDir, "emme.test-fixtures")

    val result = runTasks(projectDir)

    assertThat(result.output).contains("testFixturesJar", "BUILD SUCCESSFUL")
  }

  @Test
  fun `persistence messaging modulith and web capabilities remain independently composable`(
    @TempDir projectDir: Path,
  ) {
    writeFixtureProjects(projectDir)
    writeBuild(
      projectDir,
      """
      id("emme.persistence")
      id("emme.messaging")
      id("emme.modulith")
      id("emme.spring-web")
      """.trimIndent(),
    )

    val result = runTasks(projectDir)

    assertThat(result.output).contains("modulithDocs", "BUILD SUCCESSFUL")
  }

  private fun runTasks(projectDir: Path) =
    GradleRunner
      .create()
      .withProjectDir(projectDir.toFile())
      .withArguments("tasks", "--all", "--stacktrace")
      .build()

  private fun writeBuild(
    projectDir: Path,
    plugins: String,
  ) {
    val pluginBlock =
      if (plugins.contains("\n")) {
        plugins
      } else {
        "id(\"$plugins\")"
      }
    projectDir.resolve("build.gradle.kts").writeText(
      """
      plugins {
          $pluginBlock
      }
      """.trimIndent(),
    )
  }

  private fun writeFixtureProjects(projectDir: Path) {
    val buildLogicPath = escapePath(findBuildLogicDir())
    val catalogPath = escapePath(findCatalogPath())
    projectDir.resolve("settings.gradle.kts").writeText(
      """
      pluginManagement {
          includeBuild("$buildLogicPath")
          repositories {
              gradlePluginPortal()
              mavenCentral()
          }
      }
      dependencyResolutionManagement {
          repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
          repositories { mavenCentral() }
          versionCatalogs {
              create("libs") { from(files("$catalogPath")) }
          }
      }
      rootProject.name = "test-capabilities"
      include(":platform", ":libraries:testing", ":libraries:test-containers", ":libraries:functional")
      """.trimIndent(),
    )

    projectDir.resolve("platform").toFile().mkdirs()
    projectDir.resolve("platform/build.gradle.kts").writeText(
      """
      plugins { `java-platform` }
      javaPlatform { allowDependencies() }
      """.trimIndent(),
    )

    listOf("testing", "test-containers", "functional").forEach { name ->
      val moduleDir = projectDir.resolve("libraries/$name")
      moduleDir.toFile().mkdirs()
      moduleDir.resolve("build.gradle.kts").writeText(
        """
        plugins {
            `java-library`
            ${if (name == "testing") "`java-test-fixtures`" else ""}
        }
        """.trimIndent(),
      )
    }
  }
}
