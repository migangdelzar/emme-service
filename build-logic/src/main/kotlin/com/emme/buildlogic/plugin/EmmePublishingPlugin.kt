package com.emme.buildlogic.plugin

import com.emme.buildlogic.extension.EmmePublishingExtension
import com.emme.buildlogic.internal.TaskNames
import com.emme.buildlogic.model.ReleaseChannel
import com.emme.buildlogic.provider.publishing.GhcrPublisherProvider
import com.emme.buildlogic.provider.publishing.PublisherProvider
import com.emme.buildlogic.task.publishing.GenerateBuildInfo
import com.emme.buildlogic.task.publishing.GenerateReleaseManifest
import com.emme.buildlogic.task.publishing.GenerateSbomTask
import com.emme.buildlogic.task.publishing.SignArtifactsTask
import com.emme.buildlogic.task.publishing.VerifyReleaseVersion
import com.emme.buildlogic.value.GitBranchValueSource
import com.emme.buildlogic.value.GitCommitValueSource
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

class EmmePublishingPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    with(project) {
      val extension = extensions.create("emmePublishing", EmmePublishingExtension::class.java)
      val gitCommit = providers.of(GitCommitValueSource::class.java) {}
      val gitBranch = providers.of(GitBranchValueSource::class.java) {}
      val version = extension.version.orElse(providers.provider { project.version.toString() })

      val publisher =
        gradle.sharedServices.registerIfAbsent(
          "emmePublisher",
          GhcrPublisherProvider::class.java,
        ) {
          parameters.keyId.set(extension.signingKeyId)
          parameters.signArtifacts.set(extension.signArtifacts)
          maxParallelUsages.set(1)
        }

      tasks.register(TaskNames.PUBLISH_INFO, GenerateBuildInfo::class.java) {
        group = "publishing"
        this.version.set(version)
        commit.set(gitCommit)
        branch.set(gitBranch)
        channel.set(extension.channel.map { c: ReleaseChannel -> c.name.lowercase() })
        outputFile.set(layout.buildDirectory.file("publishing/build-info.properties"))
        onlyIf { extension.enabled.get() }
      }
      tasks.register(TaskNames.PUBLISH_MANIFEST, GenerateReleaseManifest::class.java) {
        group = "publishing"
        this.version.set(version)
        channel.set(extension.channel.map { c: ReleaseChannel -> c.name.lowercase() })
        commit.set(gitCommit)
        registry.set(extension.registry)
        manifestFile.set(layout.buildDirectory.file("publishing/manifest.yaml"))
        onlyIf { extension.enabled.get() }
      }
      tasks.register(TaskNames.PUBLISH_VERIFY, VerifyReleaseVersion::class.java) {
        group = "publishing"
        this.version.set(version)
        onlyIf { extension.enabled.get() }
      }
      tasks.register(TaskNames.PUBLISH_SIGN, SignArtifactsTask::class.java) {
        group = "publishing"
        this.artifact.set(layout.buildDirectory.file("libs/emme-studio.jar"))
        this.keyId.set(extension.signingKeyId)
        this.signatureFile.set(layout.buildDirectory.file("libs/emme-studio.jar.asc"))
        this.publisher.set(publisher)
        onlyIf { extension.signArtifacts.get() && extension.enabled.get() }
        dependsOn("bootJar")
      }
      tasks.register(TaskNames.PUBLISH_SBOM, GenerateSbomTask::class.java) {
        group = "publishing"
        sbomFile.set(layout.buildDirectory.file("reports/sbom/emme-sbom.json"))
        onlyIf { extension.enabled.get() }
      }
    }
  }
}
