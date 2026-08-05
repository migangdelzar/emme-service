package com.emme.calendar.adapter.out.google.adapter;

import com.emme.calendar.adapter.out.google.client.GoogleSheetsClient;
import com.emme.calendar.adapter.out.persistence.entity.GoogleSpreadsheetLinkEntity;
import com.emme.calendar.adapter.out.persistence.repository.SpringDataGoogleSpreadsheetLinkRepository;
import com.emme.calendar.api.result.GoogleSpreadsheetDetails;
import com.emme.calendar.application.port.out.GoogleSheetsExportPort;
import com.emme.appointments.api.result.AppointmentSummary;
import com.emme.clients.api.result.CustomerSummary;
import com.emme.appointments.api.usecase.ListAppointmentsUseCase;
import com.emme.clients.api.usecase.ListCustomersUseCase;
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
public class GoogleSheetsAdapter implements GoogleSheetsExportPort {

  private static final Logger log = LoggerFactory.getLogger(GoogleSheetsAdapter.class);
  private static final DateTimeFormatter DATE_FMT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("America/Mexico_City"));
  private static final DateTimeFormatter TIME_FMT =
      DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.of("America/Mexico_City"));

  private final GoogleSheetsClient sheetsClient;
  private final SpringDataGoogleSpreadsheetLinkRepository sheetRepo;
  private final ListAppointmentsUseCase listAppointments;
  private final ListCustomersUseCase listCustomers;

  public GoogleSheetsAdapter(
      GoogleSheetsClient sheetsClient,
      SpringDataGoogleSpreadsheetLinkRepository sheetRepo,
      ListAppointmentsUseCase listAppointments,
      ListCustomersUseCase listCustomers) {
    this.sheetsClient = sheetsClient;
    this.sheetRepo = sheetRepo;
    this.listAppointments = listAppointments;
    this.listCustomers = listCustomers;
  }

  /** Export data to a new spreadsheet. */
  public GoogleSpreadsheetDetails export(UUID tenantId, String exportType) throws Exception {
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

    var link = new GoogleSpreadsheetLinkEntity(tenantId, info.id(), info.url(), exportType);
    link.setLastExportedAt(Instant.now());
    var saved = sheetRepo.save(link);
    log.info("Exported spreadsheet: {} (type={}, tenant={})", info.id(), exportType, tenantId);
    return toResult(saved);
  }

  /** Re-export to an existing spreadsheet. */
  public GoogleSpreadsheetDetails reExport(UUID tenantId, String spreadsheetId) throws Exception {
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
    return toResult(saved);
  }

  private GoogleSpreadsheetDetails toResult(GoogleSpreadsheetLinkEntity entity) {
    return new GoogleSpreadsheetDetails(
        entity.getId(),
        entity.getTenantId(),
        entity.getSpreadsheetId(),
        entity.getSpreadsheetUrl(),
        entity.getExportType(),
        entity.getLastExportedAt(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }

  private Object[][] buildAppointmentRows(UUID tenantId) {
    List<AppointmentSummary> appointments = listAppointments.listAppointments(tenantId);
    Object[][] rows = new Object[appointments.size() + 1][];
    rows[0] = new Object[] {"Date", "Time", "Client", "Service", "Artist", "Status"};

    for (int i = 0; i < appointments.size(); i++) {
      AppointmentSummary a = appointments.get(i);
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
    List<CustomerSummary> customers = listCustomers.listCustomers(tenantId);
    Object[][] rows = new Object[customers.size() + 1][];
    rows[0] = new Object[] {"Name", "Phone", "Email", "Last Visit"};

    for (int i = 0; i < customers.size(); i++) {
      CustomerSummary c = customers.get(i);
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
