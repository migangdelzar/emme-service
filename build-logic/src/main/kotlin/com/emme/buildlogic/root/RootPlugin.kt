package com.emme.buildlogic.root

import com.emme.buildlogic.core.PluginIds
import com.emme.buildlogic.core.TaskNames
import com.emme.buildlogic.environment.EnvironmentPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

class RootPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    with(project) {
      require(this == rootProject) {
        "${PluginIds.ROOT} can only be applied to the root project"
      }

      pluginManager.apply(EnvironmentPlugin::class.java)
      extensions.create("emme", BuildExtension::class.java)

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
          TaskNames.INTEGRATION_TEST,
        )
      }

      tasks.register(TaskNames.INTEGRATION_TEST) {
        group = "verification"
        description = "Runs all integration tests"
        dependsOn(
          subprojects.flatMap { sp ->
            sp.tasks.matching { it.name == TaskNames.INTEGRATION_TEST }
          },
        )
      }
    }
  }
}
