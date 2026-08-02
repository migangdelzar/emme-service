package com.emme.calendar.adapter.out.google.client;

import com.emme.calendar.adapter.out.google.adapter.GoogleOAuthAdapter;
import com.emme.calendar.adapter.out.google.model.PersonaType;
import com.emme.calendar.configuration.GoogleHttpClient;
import com.emme.identity.adapter.in.web.security.UserContextHolder;
import com.emme.kernel.context.TenantContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GoogleSheetsClient {

  private static final Logger log = LoggerFactory.getLogger(GoogleSheetsClient.class);
  private static final String SHEETS_API = "https://sheets.googleapis.com/v4/spreadsheets";

  private final GoogleOAuthAdapter oauthService;
  private final ObjectMapper mapper;
  private final GoogleHttpClient httpClient;

  public GoogleSheetsClient(
      GoogleOAuthAdapter oauthService, ObjectMapper mapper, GoogleHttpClient httpClient) {
    this.oauthService = oauthService;
    this.mapper = mapper;
    this.httpClient = httpClient;
  }

  public record SpreadsheetInfo(String id, String url, String title) {}

  /** Create a new Google Sheets spreadsheet. */
  public SpreadsheetInfo createSpreadsheet(String title) throws Exception {
    String token = getToken();
    var body = mapper.createObjectNode();
    var props = body.putObject("properties");
    props.put("title", title);

    Request request =
        new Request.Builder()
            .url(SHEETS_API)
            .header("Authorization", "Bearer " + token)
            .post(
                RequestBody.create(
                    mapper.writeValueAsString(body), MediaType.get("application/json")))
            .build();

    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        String errorBody = response.body() != null ? response.body().string() : "";
        throw new RuntimeException(
            "Sheets create failed: HTTP " + response.code() + " — " + errorBody);
      }
      var json = mapper.readTree(response.body().string());
      var id = json.get("spreadsheetId").asText();
      var url = json.get("spreadsheetUrl").asText();
      log.info("Created spreadsheet: {} ({})", title, id);
      return new SpreadsheetInfo(id, url, title);
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

    Request request =
        new Request.Builder()
            .url(url)
            .header("Authorization", "Bearer " + token)
            .put(
                RequestBody.create(
                    mapper.writeValueAsString(body), MediaType.get("application/json")))
            .build();

    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        String errorBody = response.body() != null ? response.body().string() : "";
        throw new RuntimeException(
            "Sheets write failed: HTTP " + response.code() + " — " + errorBody);
      }
      log.info("Wrote {} rows to spreadsheetId={} range={}", values.length, spreadsheetId, range);
    }
  }

  private String getToken() {
    var tenantId = TenantContextHolder.requireCurrentTenantId();
    var userId = UserContextHolder.currentSubject();
    return oauthService.getValidAccessToken(tenantId, userId, PersonaType.STAFF);
  }
}
