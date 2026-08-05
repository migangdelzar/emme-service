package com.emme.buildlogic.publishing

import com.emme.buildlogic.core.TaskNames
import com.emme.buildlogic.git.GitBranchValueSource
import com.emme.buildlogic.git.GitCommitValueSource
import com.emme.buildlogic.model.ReleaseChannel
import com.emme.buildlogic.publishing.provider.GhcrPublisherProvider
import com.emme.buildlogic.publishing.provider.PublisherProvider
import com.emme.buildlogic.publishing.task.GenerateBuildInfoTask
import com.emme.buildlogic.publishing.task.GenerateReleaseManifestTask
import com.emme.buildlogic.publishing.task.GenerateSbomTask
import com.emme.buildlogic.publishing.task.SignArtifactsTask
import com.emme.buildlogic.publishing.task.VerifyReleaseVersionTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import java.time.Instant

class PublishingPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    with(project) {
      val extension = extensions.create("emmePublishing", PublishingExtension::class.java)
      val gitCommit = providers.of(GitCommitValueSource::class.java) {}
      val gitBranch = providers.of(GitBranchValueSource::class.java) {}
      val version = extension.version.orElse(providers.provider { project.version.toString() })
      val buildTimestamp = providers.provider { Instant.now().toString() }

      extension.registry.convention("")

      val publisher =
        gradle.sharedServices.registerIfAbsent(
          "emmePublisher",
          GhcrPublisherProvider::class.java,
        ) {
          parameters.keyId.set(extension.signingKeyId)
          parameters.signArtifacts.set(extension.signArtifacts)
          maxParallelUsages.set(1)
        }

      tasks.register(TaskNames.PUBLISH_BUILD_INFO, GenerateBuildInfoTask::class.java) {
        group = "publishing"
        this.version.set(version)
        commit.set(gitCommit)
        branch.set(gitBranch)
        channel.set(extension.channel.map { c: ReleaseChannel -> c.name.lowercase() })
        this.buildTimestamp.set(buildTimestamp)
        outputFile.set(layout.buildDirectory.file("publishing/build-info.properties"))
        onlyIf { extension.enabled.get() }
      }
      tasks.register(TaskNames.PUBLISH_MANIFEST, GenerateReleaseManifestTask::class.java) {
        group = "publishing"
        this.version.set(version)
        channel.set(extension.channel.map { c: ReleaseChannel -> c.name.lowercase() })
        commit.set(gitCommit)
        registry.set(extension.registry)
        releaseTimestamp.set(buildTimestamp)
        manifestFile.set(layout.buildDirectory.file("publishing/manifest.yaml"))
        onlyIf { extension.enabled.get() }
      }
      tasks.register(TaskNames.PUBLISH_VERIFY_VERSION, VerifyReleaseVersionTask::class.java) {
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
