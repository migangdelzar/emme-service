package com.emme.calendar.adapter.out.google;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.MockRestServiceServer.bindTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.emme.calendar.adapter.out.google.adapter.GoogleOAuthAdapter;
import com.emme.calendar.adapter.out.google.oauth.TokenEncryptionService;
import com.emme.calendar.adapter.out.persistence.repository.SpringDataGoogleOAuthTokenRepository;
import com.emme.calendar.configuration.GoogleOAuthProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

class GoogleOAuthProviderContractTest {

  @Test
  void exchangesAuthorizationCodeUsingGoogleOAuthFormContract() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = bindTo(builder).build();
    GoogleOAuthAdapter adapter = adapter(builder);
    server
        .expect(requestTo("https://oauth2.googleapis.com/token"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().formData(exchangeForm()))
        .andRespond(
            withStatus(HttpStatus.OK)
                .body(
                    "{\"access_token\":\"access-123\",\"refresh_token\":\"refresh-123\",\"scope\":\"calendar\",\"expires_in\":3600}"));

    var tokens = adapter.exchangeCode("code-123");

    assertThat(tokens.accessToken()).isEqualTo("access-123");
    assertThat(tokens.refreshToken()).isEqualTo("refresh-123");
    assertThat(tokens.expiresIn()).isEqualTo(3600);
    server.verify();
  }

  @Test
  void refreshesUsingTheDecryptedRefreshToken() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = bindTo(builder).build();
    TokenEncryptionService encryption = mock(TokenEncryptionService.class);
    when(encryption.decrypt("encrypted-refresh")).thenReturn("refresh-123");
    GoogleOAuthAdapter adapter = adapter(builder, encryption);
    server
        .expect(requestTo("https://oauth2.googleapis.com/token"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(
            content()
                .formData(
                    form(
                        "refresh_token",
                        "refresh-123",
                        "client_id",
                        "client-123",
                        "client_secret",
                        "secret-123",
                        "grant_type",
                        "refresh_token")))
        .andRespond(
            withStatus(HttpStatus.OK)
                .body("{\"access_token\":\"access-456\",\"expires_in\":1800}"));

    adapter.refreshAccessToken("encrypted-refresh");
    server.verify();
  }

  private static GoogleOAuthAdapter adapter(RestClient.Builder builder) {
    return adapter(builder, mock(TokenEncryptionService.class));
  }

  private static GoogleOAuthAdapter adapter(
      RestClient.Builder builder, TokenEncryptionService encryption) {
    return new GoogleOAuthAdapter(
        new GoogleOAuthProperties(
            "client-123",
            "secret-123",
            "https://app.test/callback",
            "12345678901234567890123456789012"),
        encryption,
        mock(SpringDataGoogleOAuthTokenRepository.class),
        new ObjectMapper(),
        builder.build());
  }

  private static MultiValueMap<String, String> exchangeForm() {
    return form(
        "code",
        "code-123",
        "client_id",
        "client-123",
        "client_secret",
        "secret-123",
        "redirect_uri",
        "https://app.test/callback",
        "grant_type",
        "authorization_code");
  }

  private static MultiValueMap<String, String> form(String... values) {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    for (int index = 0; index < values.length; index += 2) {
      form.add(values[index], values[index + 1]);
    }
    return form;
  }
}
