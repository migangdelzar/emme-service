package com.emme.buildlogic.internal

import org.gradle.api.Project

object DependencyConfiguration {
  fun addPlatform(project: Project) =
    with(project) {
      configurations.getByName("implementation").dependencies.add(
        dependencies.platform(project(":platform")),
      )
    }

  fun addTestPlatform(project: Project) =
    with(project) {
      configurations.getByName("testImplementation").dependencies.add(
        dependencies.platform(project(":platform")),
      )
    }

  fun addIntegrationTestPlatform(project: Project) =
    with(project) {
      configurations.getByName("integrationTestImplementation").dependencies.add(
        dependencies.platform(project(":platform")),
      )
    }
}
