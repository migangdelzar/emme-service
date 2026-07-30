package com.emme.calendar.infrastructure.google.application;

import com.emme.calendar.infrastructure.google.entity.GoogleSpreadsheetLink;
import com.emme.calendar.infrastructure.google.entity.GoogleSpreadsheetLinkRepository;
import com.emme.calendar.infrastructure.google.provider.GoogleSheetsClient;
import com.emme.studio.api.AppointmentInfo;
import com.emme.studio.api.CustomerInfo;
import com.emme.studio.api.SalonApi;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SheetsExportService {

  private static final Logger log = LoggerFactory.getLogger(SheetsExportService.class);
  private static final DateTimeFormatter DATE_FMT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("America/Mexico_City"));
  private static final DateTimeFormatter TIME_FMT =
      DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.of("America/Mexico_City"));

  private final GoogleSheetsClient sheetsClient;
  private final GoogleSpreadsheetLinkRepository sheetRepo;
  private final SalonApi salonApi;

  public SheetsExportService(
      GoogleSheetsClient sheetsClient,
      GoogleSpreadsheetLinkRepository sheetRepo,
      SalonApi salonApi) {
    this.sheetsClient = sheetsClient;
    this.sheetRepo = sheetRepo;
    this.salonApi = salonApi;
  }

  /** Export data to a new spreadsheet. */
  public GoogleSpreadsheetLink export(UUID tenantId, String exportType) throws Exception {
    String title =
        "Emme Nails - "
            + switch (exportType) {
              case "APPOINTMENTS" -> "Appointments";
              case "CLIENTS" -> "Clients";
              case "FULL" -> "Full Export";
              default -> "Export";
            };

    var info = sheetsClient.createSpreadsheet(title);

    if ("APPOINTMENTS".equals(exportType) || "FULL".equals(exportType)) {
      var rows = buildAppointmentRows(tenantId);
      sheetsClient.writeValues(info.id(), "Sheet1!A1", rows);
    }
    if ("CLIENTS".equals(exportType) || "FULL".equals(exportType)) {
      var rows = buildCustomerRows(tenantId);
      String range = "FULL".equals(exportType) ? "Sheet2!A1" : "Sheet1!A1";
      sheetsClient.writeValues(info.id(), range, rows);
    }

    var link = new GoogleSpreadsheetLink(tenantId, info.id(), info.url(), exportType);
    link.setLastExportedAt(Instant.now());
    var saved = sheetRepo.save(link);
    log.info("Exported spreadsheet: {} (type={}, tenant={})", info.id(), exportType, tenantId);
    return saved;
  }

  /** Re-export to an existing spreadsheet. */
  public GoogleSpreadsheetLink reExport(UUID tenantId, String spreadsheetId) throws Exception {
    var link =
        sheetRepo
            .findByTenantIdAndSpreadsheetId(tenantId, spreadsheetId)
            .orElseThrow(() -> new IllegalArgumentException("Spreadsheet not found"));

    String exportType = link.getExportType();

    if ("APPOINTMENTS".equals(exportType) || "FULL".equals(exportType)) {
      var rows = buildAppointmentRows(tenantId);
      sheetsClient.writeValues(spreadsheetId, "Sheet1!A1", rows);
    }
    if ("CLIENTS".equals(exportType) || "FULL".equals(exportType)) {
      var rows = buildCustomerRows(tenantId);
      String range = "FULL".equals(exportType) ? "Sheet2!A1" : "Sheet1!A1";
      sheetsClient.writeValues(spreadsheetId, range, rows);
    }

    link.setLastExportedAt(Instant.now());
    var saved = sheetRepo.save(link);
    log.info("Re-exported spreadsheet: {} (tenant={})", spreadsheetId, tenantId);
    return saved;
  }

  private Object[][] buildAppointmentRows(UUID tenantId) {
    List<AppointmentInfo> appointments = salonApi.listAppointments(tenantId);
    Object[][] rows = new Object[appointments.size() + 1][];
    rows[0] = new Object[] {"Date", "Time", "Client", "Service", "Artist", "Status"};

    for (int i = 0; i < appointments.size(); i++) {
      AppointmentInfo a = appointments.get(i);
      var startsAt = a.startsAt();
      rows[i + 1] =
          new Object[] {
            startsAt != null ? DATE_FMT.format(startsAt) : "",
            startsAt != null ? TIME_FMT.format(startsAt) : "",
            a.customerName() != null ? a.customerName() : "",
            a.serviceName() != null ? a.serviceName() : "",
            a.artistName() != null ? a.artistName() : "",
            a.status() != null ? a.status() : ""
          };
    }

    log.info(
        "Built {} appointment rows for tenant={} ({} data rows)",
        rows.length,
        tenantId,
        appointments.size());
    return rows;
  }

  private Object[][] buildCustomerRows(UUID tenantId) {
    List<CustomerInfo> customers = salonApi.listCustomers(tenantId);
    Object[][] rows = new Object[customers.size() + 1][];
    rows[0] = new Object[] {"Name", "Phone", "Email", "Last Visit"};

    for (int i = 0; i < customers.size(); i++) {
      CustomerInfo c = customers.get(i);
      rows[i + 1] =
          new Object[] {
            c.name() != null ? c.name() : "",
            c.phone() != null ? c.phone() : "",
            c.email() != null ? c.email() : "",
            "N/A"
          };
    }

    log.info(
        "Built {} customer rows for tenant={} ({} data rows)",
        rows.length,
        tenantId,
        customers.size());
    return rows;
  }
}
