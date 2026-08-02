package com.emme.buildlogic.core

import org.gradle.api.invocation.Gradle
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.services.BuildServiceSpec

object ProviderRegistry {
  inline fun <reified T : BuildService<P>, P : BuildServiceParameters> registerProvider(
    gradle: Gradle,
    name: String,
    noinline configure: BuildServiceSpec<P>.() -> Unit = {},
  ): Provider<T> = gradle.sharedServices.registerIfAbsent(name, T::class.java, configure)

  fun <P : BuildServiceParameters> BuildServiceSpec<P>.containerConcurrency() {
    maxParallelUsages.set(2)
  }

  fun <P : BuildServiceParameters> BuildServiceSpec<P>.singleConcurrency() {
    maxParallelUsages.set(1)
  }
}
