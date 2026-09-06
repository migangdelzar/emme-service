package com.emme.calendar.adapter.out.google.client;

import com.emme.calendar.adapter.out.google.adapter.GoogleOAuthAdapter;
import com.emme.calendar.adapter.out.google.model.PersonaType;
import com.emme.kernel.context.TenantContextHolder;
import com.emme.shared.web.security.CurrentUserContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class GoogleSheetsClient {

  private static final Logger log = LoggerFactory.getLogger(GoogleSheetsClient.class);
  private static final String SHEETS_API = "https://sheets.googleapis.com/v4/spreadsheets";

  private final GoogleOAuthAdapter oauthService;
  private final ObjectMapper mapper;
  private final RestClient httpClient;

  public GoogleSheetsClient(
      GoogleOAuthAdapter oauthService,
      ObjectMapper mapper,
      @Qualifier("googleRestClient") RestClient httpClient) {
    this.oauthService = oauthService;
    this.mapper = mapper;
    this.httpClient = httpClient;
  }

  public record SpreadsheetDetails(String id, String url, String title) {}

  /** Create a new Google Sheets spreadsheet. */
  public SpreadsheetDetails createSpreadsheet(String title) throws Exception {
    String token = getToken();
    var body = mapper.createObjectNode();
    var props = body.putObject("properties");
    props.put("title", title);

    try {
      String responseBody =
          httpClient
              .post()
              .uri(SHEETS_API)
              .header("Authorization", "Bearer " + token)
              .contentType(MediaType.APPLICATION_JSON)
              .body(mapper.writeValueAsString(body))
              .retrieve()
              .body(String.class);
      var json = mapper.readTree(responseBody == null ? "" : responseBody);
      var id = json.get("spreadsheetId").asText();
      var url = json.get("spreadsheetUrl").asText();
      log.info("Created spreadsheet: {} ({})", title, id);
      return new SpreadsheetDetails(id, url, title);
    } catch (RestClientResponseException e) {
      throw new RuntimeException(
          "Sheets create failed: HTTP "
              + e.getStatusCode().value()
              + " — "
              + e.getResponseBodyAsString(),
          e);
    }
  }

  /** Write rows of data to a sheet range using valueInputOption=USER_ENTERED. */
  public void writeValues(String spreadsheetId, String range, Object[][] values) throws Exception {
    String token = getToken();
    var body = mapper.createObjectNode();
    body.put("range", range);
    body.put("majorDimension", "ROWS");
    ArrayNode rows = mapper.createArrayNode();
    for (Object[] row : values) {
      ArrayNode rowNode = mapper.createArrayNode();
      for (Object cell : row) rowNode.add(String.valueOf(cell != null ? cell : ""));
      rows.add(rowNode);
    }
    body.set("values", rows);

    String url =
        SHEETS_API + "/" + spreadsheetId + "/values/" + range + "?valueInputOption=USER_ENTERED";

    try {
      httpClient
          .put()
          .uri(url)
          .header("Authorization", "Bearer " + token)
          .contentType(MediaType.APPLICATION_JSON)
          .body(mapper.writeValueAsString(body))
          .retrieve()
          .toBodilessEntity();
      log.info("Wrote {} rows to spreadsheetId={} range={}", values.length, spreadsheetId, range);
    } catch (RestClientResponseException e) {
      throw new RuntimeException(
          "Sheets write failed: HTTP "
              + e.getStatusCode().value()
              + " — "
              + e.getResponseBodyAsString(),
          e);
    }
  }

  private String getToken() {
    var tenantId = TenantContextHolder.requireCurrentTenantId();
    var userId = CurrentUserContextHolder.currentSubject();
    return oauthService.getValidAccessToken(tenantId, userId, PersonaType.STAFF);
  }
}
