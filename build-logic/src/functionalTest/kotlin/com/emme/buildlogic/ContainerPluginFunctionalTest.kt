package com.emme.buildlogic

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

class ContainerPluginFunctionalTest {

    @Test
    fun `registers container tasks`(@TempDir projectDir: Path) {
        writeSettings(projectDir)

        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("emme.java-base")
                id("emme.container")
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withArguments("tasks", "--group=container", "--stacktrace")
            .build()

        assertThat(result.output).contains("containerBuild")
    }

    @Test
    fun `container task is disabled by default`(@TempDir projectDir: Path) {
        writeSettings(projectDir)

        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("emme.java-base")
                id("emme.container")
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withArguments("containerBuild", "--stacktrace")
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
            rootProject.name = "test-container"
            """.trimIndent()
        )
    }
}
