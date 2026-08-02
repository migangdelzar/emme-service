package com.emme.buildlogic

import com.emme.buildlogic.container.EmmeContainerExtension
import com.emme.buildlogic.container.EmmeContainerPlugin
import com.emme.buildlogic.deployment.EmmeDeploymentExtension
import com.emme.buildlogic.deployment.EmmeDeploymentPlugin
import com.emme.buildlogic.publishing.EmmePublishingExtension
import com.emme.buildlogic.publishing.EmmePublishingPlugin
import com.emme.buildlogic.root.EmmeBuildExtension
import com.emme.buildlogic.root.EmmeRootPlugin
import com.emme.buildlogic.security.EmmeSecurityExtension
import com.emme.buildlogic.security.EmmeSecurityPlugin
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class PluginRegistrationTest {
  private fun project(): Project = ProjectBuilder.builder().build()

  @Test
  fun `emme root plugin registers extension`() {
    val project = project()
    project.pluginManager.apply(EmmeRootPlugin::class.java)

    val ext = project.extensions.findByType(EmmeBuildExtension::class.java)
    assertThat(ext).isNotNull
  }

  @Test
  fun `emme root plugin registers CI task`() {
    val project = project()
    project.pluginManager.apply(EmmeRootPlugin::class.java)

    assertThat(project.tasks.findByName("ci")).isNotNull
    assertThat(project.tasks.findByName("full")).isNotNull
  }

  @Test
  fun `emme container plugin registers extension`() {
    val project = project()
    project.pluginManager.apply(EmmeContainerPlugin::class.java)

    val ext = project.extensions.findByType(EmmeContainerExtension::class.java)
    assertThat(ext).isNotNull
    assertThat(ext!!.enabled.get()).isFalse()
    assertThat(ext.imageTags.get()).contains("latest")
  }

  @Test
  fun `emme container plugin registers tasks`() {
    val project = project()
    project.pluginManager.apply(EmmeContainerPlugin::class.java)

    assertThat(project.tasks.findByName("containerBuild")).isNotNull
    assertThat(project.tasks.findByName("containerPush")).isNotNull
    assertThat(project.tasks.findByName("containerVerify")).isNotNull
  }

  @Test
  fun `emme publishing plugin registers extension`() {
    val project = project()
    project.pluginManager.apply(EmmePublishingPlugin::class.java)

    val ext = project.extensions.findByType(EmmePublishingExtension::class.java)
    assertThat(ext).isNotNull
    assertThat(ext!!.enabled.get()).isFalse()
  }

  @Test
  fun `emme publishing plugin registers tasks`() {
    val project = project()
    project.pluginManager.apply(EmmePublishingPlugin::class.java)

    assertThat(project.tasks.findByName("publishBuildInfo")).isNotNull
    assertThat(project.tasks.findByName("publishManifest")).isNotNull
    assertThat(project.tasks.findByName("publishVerifyVersion")).isNotNull
    assertThat(project.tasks.findByName("publishSign")).isNotNull
    assertThat(project.tasks.findByName("publishSbom")).isNotNull
  }

  @Test
  fun `emme deployment plugin registers extension with defaults`() {
    val project = project()
    project.pluginManager.apply(EmmeDeploymentPlugin::class.java)

    val ext = project.extensions.findByType(EmmeDeploymentExtension::class.java)
    assertThat(ext).isNotNull
  }

  @Test
  fun `emme security plugin registers extension`() {
    val project = project()
    project.pluginManager.apply(EmmeSecurityPlugin::class.java)

    assertThat(project.extensions.findByType(EmmeSecurityExtension::class.java)).isNotNull
  }
}
