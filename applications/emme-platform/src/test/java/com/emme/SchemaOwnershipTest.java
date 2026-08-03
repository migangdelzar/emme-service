package com.emme;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Data-layer boundaries: emme_core belongs to identity + tenancy; every studio table has exactly
 * one owning module.
 */
class SchemaOwnershipTest {

  private static final JavaClasses CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("com.emme");

  private static final Set<String> CORE_SCHEMA_OWNERS = Set.of("identity", "tenancy");

  @Test
  void emmeCoreTablesAreOwnedByPlatformModules() {
    classes()
        .that(declareTableSchema("emme_core"))
        .should()
        .resideInAnyPackage("com.emme.identity..", "com.emme.tenancy..")
        .because("only identity and tenancy own the emme_core schema")
        .check(CLASSES);
  }

  @Test
  void everyEntityDeclaresAnExplicitTableWithExactlyOneOwningModule() {
    Map<String, Set<String>> tableOwners = new HashMap<>();
    for (JavaClass clazz : CLASSES) {
      if (!clazz.isAnnotatedWith(Entity.class)) {
        continue;
      }
      Optional<Table> table = clazz.tryGetAnnotationOfType(Table.class);
      assertThat(table)
          .as("%s must declare @Table with an explicit name", clazz.getName())
          .isPresent();
      assertThat(table.get().name())
          .as("%s must declare an explicit table name", clazz.getName())
          .isNotBlank();
      String qualified =
          table.get().schema().isBlank()
              ? table.get().name()
              : table.get().schema() + "." + table.get().name();
      tableOwners.computeIfAbsent(qualified, k -> new HashSet<>()).add(moduleOf(clazz));
    }
    assertThat(tableOwners).isNotEmpty();
    tableOwners.forEach(
        (tableName, owners) ->
            assertThat(owners)
                .as("table %s must be mapped by exactly one module", tableName)
                .hasSize(1));
  }

  @Test
  void emmeCoreIsNotReferencedOutsideOwningModules() throws IOException {
    Path root = Path.of("src/main/java/com/emme");
    try (Stream<Path> files = Files.walk(root)) {
      List<String> offenders =
          files
              .filter(p -> p.toString().endsWith(".java"))
              .filter(
                  p -> {
                    String module = root.relativize(p).getName(0).toString();
                    return !CORE_SCHEMA_OWNERS.contains(module);
                  })
              .filter(p -> readFile(p).contains("emme_core"))
              .map(p -> root.relativize(p).toString())
              .toList();
      assertThat(offenders)
          .as("only identity/tenancy may reference the emme_core schema (SQL or mappings)")
          .isEmpty();
    }
  }

  private static String readFile(Path path) {
    try {
      return Files.readString(path);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static String moduleOf(JavaClass clazz) {
    String remainder = clazz.getPackageName().substring("com.emme".length());
    return remainder.isEmpty() ? "(root)" : remainder.substring(1).split("\\.")[0];
  }

  private static DescribedPredicate<JavaClass> declareTableSchema(String schema) {
    return new DescribedPredicate<>("declare @Table(schema = \"%s\")".formatted(schema)) {
      @Override
      public boolean test(JavaClass clazz) {
        return clazz
            .tryGetAnnotationOfType(Table.class)
            .map(table -> schema.equals(table.schema()))
            .orElse(false);
      }
    };
  }
}
