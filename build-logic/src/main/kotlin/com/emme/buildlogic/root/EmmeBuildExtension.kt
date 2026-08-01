package com.emme.buildlogic.root

import com.emme.buildlogic.container.EmmeContainerExtension
import com.emme.buildlogic.model.EmmeModuleType
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class EmmeBuildExtension
  @Inject
  constructor(
    objects: ObjectFactory,
  ) {
    val moduleType: Property<EmmeModuleType> =
      objects.property(EmmeModuleType::class.java)

    val strictCompilation: Property<Boolean> =
      objects
        .property(Boolean::class.java)
        .convention(true)

    val container: EmmeContainerExtension =
      objects.newInstance(EmmeContainerExtension::class.java)
  }
