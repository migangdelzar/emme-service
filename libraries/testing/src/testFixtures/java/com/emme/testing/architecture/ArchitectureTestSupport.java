package com.emme.testing.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

/** Shared classpath import configuration for production architecture tests. */
public final class ArchitectureTestSupport {

  private ArchitectureTestSupport() {}

  /** Imports production classes while excluding test and generated classes. */
  public static JavaClasses productionClasses(String... packages) {
    return new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages(packages);
  }
}
