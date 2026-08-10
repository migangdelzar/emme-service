package com.emme.identity.adapter.in.web.controller;

import com.emme.identity.adapter.in.web.mapper.CustomerWebMapper;
import com.emme.identity.adapter.in.web.mapper.IdentityWebMapper;
import com.emme.identity.adapter.in.web.request.LoginRequest;
import com.emme.identity.adapter.in.web.response.TokenLoginResponse;
import com.emme.identity.api.command.AuthenticateCustomerCommand;
import com.emme.identity.api.command.AuthenticateUserCommand;
import com.emme.identity.api.command.UpdateCustomerPhoneCommand;
import com.emme.identity.api.exception.IdentityAuthenticationException;
import com.emme.identity.api.query.GetCurrentUserQuery;
import com.emme.identity.api.query.GetUserClaimsQuery;
import com.emme.identity.api.usecase.AuthenticateCustomerUseCase;
import com.emme.identity.api.usecase.AuthenticateUserUseCase;
import com.emme.identity.api.usecase.GetCurrentUserUseCase;
import com.emme.identity.api.usecase.UpdateCustomerProfileUseCase;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

  private static final Logger log = LoggerFactory.getLogger(AuthController.class);
  private final AuthenticateUserUseCase authenticateUserUseCase;
  private final AuthenticateCustomerUseCase authenticateCustomerUseCase;
  private final UpdateCustomerProfileUseCase updateCustomerProfileUseCase;
  private final GetCurrentUserUseCase getCurrentUser;

  public AuthController(
      AuthenticateUserUseCase authenticateUserUseCase,
      AuthenticateCustomerUseCase authenticateCustomerUseCase,
      UpdateCustomerProfileUseCase updateCustomerProfileUseCase,
      GetCurrentUserUseCase getCurrentUser) {
    this.authenticateUserUseCase = authenticateUserUseCase;
    this.authenticateCustomerUseCase = authenticateCustomerUseCase;
    this.updateCustomerProfileUseCase = updateCustomerProfileUseCase;
    this.getCurrentUser = getCurrentUser;
  }

  @PostMapping(path = "/api/auth/login", version = "1.0")
  public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
    try {
      var tokens =
          authenticateUserUseCase.authenticate(
              new AuthenticateUserCommand(request.email(), request.password()));

      // Get full user claims from Keycloak userinfo (access token has no claims for public clients)
      Map<String, Object> userClaims =
          authenticateUserUseCase
              .getUserClaims(new GetUserClaimsQuery(tokens.accessToken()))
              .claims();

      // Build a Jwt from userinfo claims so the authenticated user context can be reconstructed.
      String sub = (String) userClaims.get("sub");
      if (sub == null || sub.isBlank()) {
        log.error("No sub claim in userinfo response for {}", request.email());
        return ResponseEntity.status(500).body(Map.of("error", "Authentication failed"));
      }

      Jwt jwt =
          Jwt.withTokenValue(tokens.accessToken())
              .header("alg", "RS256")
              .claim("sub", sub)
              .claim(
                  "preferred_username",
                  userClaims.getOrDefault("preferred_username", request.email()))
              .claim(
                  "email",
                  userClaims.getOrDefault(
                      "email", request.email().contains("@") ? request.email() : ""))
              .claim(
                  "name",
                  userClaims.getOrDefault(
                      "name", userClaims.getOrDefault("preferred_username", request.email())))
              .claim("realm_access", userClaims.get("realm_access"))
              .claim("tenant_id", userClaims.get("tenant_id"))
              .claim("tenant_slug", userClaims.get("tenant_slug"))
              .subject(sub)
              .issuedAt(java.time.Instant.now())
              .expiresAt(java.time.Instant.now().plusSeconds(3600))
              .build();

      log.info("User {} ({}) logged in via password grant", sub, request.email());

      var user =
          IdentityWebMapper.toCurrentUserResponse(
              getCurrentUser.get(
                  new GetCurrentUserQuery(
                      sub,
                      jwt.getClaimAsString("email"),
                      jwt.getClaimAsString("name"),
                      parseTenantId(jwt.getClaim("tenant_id")))));

      // The resource server validates access-token audience and scopes. The ID token is only an
      // identity assertion for the client and must never be used as the API bearer credential.
      return ResponseEntity.ok(
          new TokenLoginResponse(tokens.accessToken(), tokens.refreshToken(), user));

    } catch (IdentityAuthenticationException e) {
      return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
    } catch (Exception e) {
      log.error("Login error", e);
      return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
    }
  }

  @PostMapping(path = "/api/auth/customer-login", version = "1.0")
  public ResponseEntity<?> customerLogin(
      @RequestBody(required = false) Map<String, String> body,
      @RequestHeader(value = "X-Provider-Token", required = false) String headerToken,
      @RequestParam(value = "token", required = false) String queryToken) {
    String providerToken =
        queryToken != null
            ? queryToken
            : (headerToken != null
                ? headerToken
                : (body != null ? body.get("providerToken") : null));
    if (providerToken == null || providerToken.isBlank()) {
      return ResponseEntity.badRequest().body(Map.of("error", "providerToken required"));
    }
    // Decode if encoded (client workaround for Spring Security JWT scanning)
    if (!providerToken.contains(".")) {
      String decoded = null;
      try {
        byte[] bytes = new byte[providerToken.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
          bytes[i] = (byte) Integer.parseInt(providerToken.substring(i * 2, i * 2 + 2), 16);
        }
        decoded = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        log.debug("Hex decoded token: {} chars", decoded.length());
      } catch (Exception hexErr) {
        log.debug("Hex decode failed: {}", hexErr.getMessage());
        try {
          decoded =
              new String(
                  java.util.Base64.getDecoder().decode(providerToken),
                  java.nio.charset.StandardCharsets.UTF_8);
          log.debug("Base64 decoded token: {} chars", decoded.length());
        } catch (Exception b64Err) {
          log.debug("Base64 decode failed: {}", b64Err.getMessage());
        }
      }
      if (decoded != null) providerToken = decoded;
    }
    try {
      var result =
          authenticateCustomerUseCase.authenticate(new AuthenticateCustomerCommand(providerToken));
      return ResponseEntity.ok(CustomerWebMapper.toLoginResponse(result));
    } catch (Exception e) {
      log.error("Customer login failed", e);
      return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
    }
  }

  @PutMapping(path = "/api/me/profile", version = "1.0")
  public ResponseEntity<?> updateCustomerProfile(
      @RequestBody Map<String, String> body, @AuthenticationPrincipal Jwt jwt) {
    if (jwt == null) return ResponseEntity.status(401).build();
    String role = jwt.getClaimAsString("role");
    if (!"CUSTOMER".equals(role)) {
      return ResponseEntity.status(403).body(Map.of("error", "Not a customer account"));
    }
    String customerId = jwt.getSubject();
    String phone = body.get("phone");
    if (phone == null || phone.isBlank()) {
      return ResponseEntity.badRequest().body(Map.of("error", "phone required"));
    }
    var customer =
        updateCustomerProfileUseCase.updatePhone(
            new UpdateCustomerPhoneCommand(UUID.fromString(customerId), phone));
    return ResponseEntity.ok(Map.of("phone", customer.phone()));
  }

  private static UUID parseTenantId(Object rawTenantId) {
    if (rawTenantId == null) return null;
    try {
      return UUID.fromString(rawTenantId.toString());
    } catch (IllegalArgumentException ignored) {
      return null;
    }
  }
}
