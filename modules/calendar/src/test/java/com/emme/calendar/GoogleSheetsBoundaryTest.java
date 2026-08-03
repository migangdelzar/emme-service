package com.emme.calendar;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GoogleSheetsBoundaryTest {

  private static final Path ROOT = Path.of("src/main/java/com/emme/calendar");

  @Test
  void keepsTheInboundSheetsAdapterIndependentOfGoogleAndPersistenceTypes() throws Exception {
    assertThat(Files.exists(ROOT.resolve("application/port/out/GoogleSheetsExportPort.java")))
        .isTrue();
    assertThat(
            Files.exists(ROOT.resolve("application/port/out/GoogleSpreadsheetLinkQueryPort.java")))
        .isTrue();
    assertThat(Files.readString(ROOT.resolve("adapter/in/web/controller/SheetsController.java")))
        .doesNotContain("adapter.out.google")
        .doesNotContain("adapter.out.persistence");
  }
}
