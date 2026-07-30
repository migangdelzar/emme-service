package com.emme.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Provides minimal OAuth2 beans for tests when {@code OAuth2ClientAutoConfiguration} and {@code
 * OAuth2ResourceServerAutoConfiguration} are excluded.
 *
 * <p>Supplies a dummy {@code ClientRegistrationRepository} so {@code SecurityConfig.oauth2Login()}
 * works, a symmetric-key {@code JwtDecoder} so {@code SecurityConfig.oauth2ResourceServer()} works
 * without reaching a real issuer, and an {@link ObjectMapper} for modules that parse JSON outside
 * the web layer.
 */
@TestConfiguration
public class TestSecurityConfig {

  @Bean
  @Primary
  public ClientRegistrationRepository clientRegistrationRepository() {
    ClientRegistration registration =
        ClientRegistration.withRegistrationId("keycloak")
            .clientId("test-client")
            .clientSecret("test-secret")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("http://localhost:8081/login/oauth2/code/keycloak")
            .scope("openid", "profile", "email")
            .authorizationUri("https://test-issuer/realms/emme/protocol/openid-connect/auth")
            .tokenUri("https://test-issuer/realms/emme/protocol/openid-connect/token")
            .userInfoUri("https://test-issuer/realms/emme/protocol/openid-connect/userinfo")
            .jwkSetUri("https://test-issuer/realms/emme/protocol/openid-connect/certs")
            .issuerUri("https://test-issuer/realms/emme")
            .userNameAttributeName("preferred_username")
            .build();
    return new InMemoryClientRegistrationRepository(registration);
  }

  @Bean
  @Primary
  public JwtDecoder jwtDecoder() {
    // 256-bit key for HS256 — only used for creating/validating test JWTs
    byte[] keyBytes = new byte[32];
    for (int i = 0; i < 32; i++) keyBytes[i] = (byte) i;
    SecretKey secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
    return NimbusJwtDecoder.withSecretKey(secretKey)
        .macAlgorithm(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256)
        .build();
  }

  @Bean
  @Primary
  public ObjectMapper objectMapper() {
    return new ObjectMapper();
  }

  @Bean
  @Primary
  @SuppressWarnings("unchecked")
  public StringRedisTemplate stringRedisTemplate() {
    var mock = Mockito.mock(StringRedisTemplate.class);
    var valueOps = (ValueOperations<String, String>) Mockito.mock(ValueOperations.class);
    Mockito.when(mock.opsForValue()).thenReturn(valueOps);
    Mockito.when(valueOps.getAndDelete(Mockito.anyString())).thenReturn(null);
    return mock;
  }
}
