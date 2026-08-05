package com.emme.buildlogic.root

import com.emme.buildlogic.model.ModuleType
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class BuildExtension
  @Inject
  constructor(
    objects: ObjectFactory,
  ) {
    val moduleType: Property<ModuleType> =
      objects.property(ModuleType::class.java)

    val strictCompilation: Property<Boolean> =
      objects
        .property(Boolean::class.java)
        .convention(true)
  }
