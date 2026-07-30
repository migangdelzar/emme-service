package com.emme.buildlogic.provider.registry

import com.emme.buildlogic.provider.container.PushResult
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

abstract class RegistryProvider :
    BuildService<RegistryProvider.Params>,
    AutoCloseable {

    interface Params : BuildServiceParameters {
        val url: Property<String>
        val username: Property<String>
        val password: Property<String>
    }

    abstract fun login(): LoginResult
    abstract fun push(image: String): PushResult
    abstract fun manifest(image: String): ManifestResult

    override fun close() = Unit
}
