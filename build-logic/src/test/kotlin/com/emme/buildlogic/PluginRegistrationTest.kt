package com.emme.buildlogic

import com.emme.buildlogic.container.ContainerExtension
import com.emme.buildlogic.container.ContainerPlugin
import com.emme.buildlogic.deployment.DeploymentExtension
import com.emme.buildlogic.deployment.DeploymentPlugin
import com.emme.buildlogic.publishing.PublishingExtension
import com.emme.buildlogic.publishing.PublishingPlugin
import com.emme.buildlogic.root.BuildExtension
import com.emme.buildlogic.root.RootPlugin
import com.emme.buildlogic.security.SecurityExtension
import com.emme.buildlogic.security.SecurityPlugin
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class PluginRegistrationTest {
  private fun project(): Project = ProjectBuilder.builder().build()

  @Test
  fun `emme root plugin registers extension`() {
    val project = project()
    project.pluginManager.apply(RootPlugin::class.java)

    val ext = project.extensions.findByType(BuildExtension::class.java)
    assertThat(ext).isNotNull
  }

  @Test
  fun `emme root plugin registers CI task`() {
    val project = project()
    project.pluginManager.apply(RootPlugin::class.java)

    assertThat(project.tasks.findByName("ci")).isNotNull
    assertThat(project.tasks.findByName("full")).isNotNull
  }

  @Test
  fun `emme container plugin registers extension`() {
    val project = project()
    project.pluginManager.apply(ContainerPlugin::class.java)

    val ext = project.extensions.findByType(ContainerExtension::class.java)
    assertThat(ext).isNotNull
    assertThat(ext!!.enabled.get()).isFalse()
    assertThat(ext.imageTags.get()).contains("latest")
  }

  @Test
  fun `emme container plugin registers tasks`() {
    val project = project()
    project.pluginManager.apply(ContainerPlugin::class.java)

    assertThat(project.tasks.findByName("containerBuild")).isNotNull
    assertThat(project.tasks.findByName("containerPush")).isNotNull
    assertThat(project.tasks.findByName("containerVerify")).isNotNull
  }

  @Test
  fun `emme publishing plugin registers extension`() {
    val project = project()
    project.pluginManager.apply(PublishingPlugin::class.java)

    val ext = project.extensions.findByType(PublishingExtension::class.java)
    assertThat(ext).isNotNull
    assertThat(ext!!.enabled.get()).isFalse()
  }

  @Test
  fun `emme publishing plugin registers tasks`() {
    val project = project()
    project.pluginManager.apply(PublishingPlugin::class.java)

    assertThat(project.tasks.findByName("publishBuildInfo")).isNotNull
    assertThat(project.tasks.findByName("publishManifest")).isNotNull
    assertThat(project.tasks.findByName("publishVerifyVersion")).isNotNull
    assertThat(project.tasks.findByName("publishSign")).isNotNull
    assertThat(project.tasks.findByName("publishSbom")).isNotNull
  }

  @Test
  fun `emme deployment plugin registers extension with defaults`() {
    val project = project()
    project.pluginManager.apply(DeploymentPlugin::class.java)

    val ext = project.extensions.findByType(DeploymentExtension::class.java)
    assertThat(ext).isNotNull
    assertThat(ext!!.runtime.get()).isEqualTo("jvm")
  }

  @Test
  fun `emme security plugin registers extension`() {
    val project = project()
    project.pluginManager.apply(SecurityPlugin::class.java)

    assertThat(project.extensions.findByType(SecurityExtension::class.java)).isNotNull
  }
}
