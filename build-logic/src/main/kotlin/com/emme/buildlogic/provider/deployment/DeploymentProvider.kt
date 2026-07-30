package com.emme.buildlogic.provider.deployment

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

abstract class DeploymentProvider :
    BuildService<DeploymentProvider.Params>,
    AutoCloseable {

    interface Params : BuildServiceParameters {
        val profile: Property<String>
        val namespace: Property<String>
        val deploymentDir: DirectoryProperty
    }

    abstract fun up(): DeployResult
    abstract fun down(): DeployResult
    abstract fun apply(): DeployResult
    abstract fun status(): StatusResult
    abstract fun logs(tail: Int): String

    override fun close() = Unit
}
