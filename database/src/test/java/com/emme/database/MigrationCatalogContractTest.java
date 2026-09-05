package com.emme.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class MigrationCatalogContractTest {

  private static final Pattern INCLUDED_SQL = Pattern.compile("file:\\s*(\\S+\\.sql)");
  private static final Path CHANGELOG_ROOT = Path.of("src/main/resources/db");

  @Test
  void everyCoreAndStudioChangelogIncludeResolvesToOneMigrationFile() throws Exception {
    List<String> includedFiles =
        Stream.concat(
                includedFiles("emme-core/changelog.yaml"),
                includedFiles("emme-studio/changelog.yaml"))
            .toList();

    assertThat(includedFiles).doesNotHaveDuplicates();
    assertThat(includedFiles).isNotEmpty();
    assertThat(includedFiles)
        .allSatisfy(
            migration ->
                assertThat(Files.exists(CHANGELOG_ROOT.resolve(migration)))
                    .as("Liquibase include must resolve: %s", migration)
                    .isTrue());
  }

  private static Stream<String> includedFiles(String changelog) throws Exception {
    Path changelogDirectory = Path.of(changelog).getParent();
    return INCLUDED_SQL
        .matcher(Files.readString(CHANGELOG_ROOT.resolve(changelog)))
        .results()
        .map(match -> changelogDirectory.resolve(match.group(1)).toString());
  }
}
