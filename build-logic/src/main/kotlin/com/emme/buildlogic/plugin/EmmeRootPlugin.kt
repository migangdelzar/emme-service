package com.emme.buildlogic.plugin

import com.emme.buildlogic.extension.EmmeBuildExtension
import com.emme.buildlogic.internal.PluginIds
import com.emme.buildlogic.internal.TaskNames
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

class EmmeRootPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        with(project) {
            require(this == rootProject) {
                "${PluginIds.ROOT} can only be applied to the root project"
            }

            extensions.create("emme", EmmeBuildExtension::class.java)

            pluginManager.apply("base")

            tasks.register(TaskNames.CI) {
                group = "verification"
                description = "Runs the standard Emme CI lifecycle."
                dependsOn("check")
            }

            tasks.register(TaskNames.FULL) {
                group = "build"
                description = "Runs verification and assembles Emme."
                dependsOn(
                    TaskNames.CI,
                    TaskNames.INTEGRATION_TEST
                )
            }

            tasks.register(TaskNames.INTEGRATION_TEST) {
                group = "verification"
                description = "Runs all integration tests"
                dependsOn(
                    subprojects.flatMap { sp ->
                        sp.tasks.matching { it.name == TaskNames.INTEGRATION_TEST }
                    }
                )
            }
        }
    }
}
