package com.emme.buildlogic

import java.nio.file.Path
import kotlin.io.path.writeText

fun findBuildLogicDir(): Path {
  val cwd = Path.of(System.getProperty("user.dir"))
  var path = cwd
  while (path != null) {
    val candidate = path.resolve("build-logic")
    if (candidate.toFile().isDirectory) {
      return candidate.toAbsolutePath()
    }
    path = path.parent
  }
  return Path.of(System.getProperty("user.dir")).resolve("build-logic").toAbsolutePath()
}

fun findBuildLogicSettingsDir(): Path {
  val cwd = Path.of(System.getProperty("user.dir"))
  var path: Path? = cwd
  while (path != null) {
    val candidate = path.resolve("build-logic-settings")
    if (candidate.toFile().isDirectory) {
      return candidate.toAbsolutePath()
    }
    path = path.parent
  }
  return Path.of(System.getProperty("user.dir")).resolve("build-logic-settings").toAbsolutePath()
}

fun findCatalogPath(): Path {
  val cwd = Path.of(System.getProperty("user.dir"))
  var path = cwd
  while (path != null) {
    val candidate = path.resolve("gradle/libs.versions.toml")
    if (candidate.toFile().exists()) {
      return candidate.toAbsolutePath()
    }
    path = path.parent
  }
  return Path.of(System.getProperty("user.dir")).resolve("gradle/libs.versions.toml").toAbsolutePath()
}

fun escapePath(path: Path): String = path.toString().replace("\\", "\\\\")

fun writeTestFixtureProject(projectDir: Path) {
  projectDir.resolve("libraries/testing").toFile().mkdirs()
  projectDir.resolve("libraries/testing/build.gradle.kts").writeText(
    """
    plugins {
        `java-library`
        `java-test-fixtures`
    }
    """.trimIndent(),
  )
}

fun writeTestContainersProject(projectDir: Path) {
  projectDir.resolve("libraries/test-containers").toFile().mkdirs()
  projectDir.resolve("libraries/test-containers/build.gradle.kts").writeText(
    """
    plugins {
        `java-library`
    }
    """.trimIndent(),
  )
}

fun writeFunctionalProject(projectDir: Path) {
  projectDir.resolve("libraries/functional").toFile().mkdirs()
  projectDir.resolve("libraries/functional/build.gradle.kts").writeText(
    """
    plugins {
        `java-library`
    }
    """.trimIndent(),
  )
}

fun dependencyRepositories(): String =
  """
  dependencyResolutionManagement {
      repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
      repositories {
          mavenCentral()
      }
  }
  """.trimIndent()
