package com.emme.assistant.ai.adapter.in.web.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.emme.ai.contracts.image.TenantImageWriter;
import com.emme.assistant.ai.adapter.in.web.security.AiWebExecutionContextFactory;
import com.emme.assistant.ai.adapter.in.web.security.AiPrincipalIdentity;
import com.emme.assistant.api.query.GetConversationQuery;
import com.emme.assistant.ai.api.result.QuoteWorkflowResult;
import com.emme.assistant.ai.api.usecase.ProcessDesignQuoteUseCase;
import com.emme.assistant.ai.application.port.out.DesignImageMetadataRepository;
import com.emme.assistant.ai.domain.workflow.QuoteWorkflowState;
import com.emme.assistant.api.result.ConversationDetails;
import com.emme.assistant.api.type.ConversationStatus;
import com.emme.assistant.api.usecase.GetConversationUseCase;
import com.emme.ai.contracts.tenant.AiAuthorizationContextResolver;
import com.emme.kernel.context.TenantContextHolder;
import com.emme.kernel.type.ChannelType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class DesignQuoteControllerTest {
  @Test
  void initializesWorkflowBeforePersistingImageMetadataAndProcessingQuote() throws Exception {
    var storage = mock(TenantImageWriter.class);
    when(storage.store(any(), any())).thenReturn("tenant/image.img");
    var metadata = mock(DesignImageMetadataRepository.class);
    var quote = mock(ProcessDesignQuoteUseCase.class);
    var conversations = mock(GetConversationUseCase.class);
    var workflowId = java.util.UUID.randomUUID();
    when(quote.process(any()))
        .thenReturn(
            new QuoteWorkflowResult(
                workflowId,
                QuoteWorkflowState.QUOTE_READY,
                java.util.Optional.empty(),
                java.util.Optional.empty()));
    var jwt = jwt();
    var image = new MockMultipartFile("image", "design.jpg", "image/jpeg", new byte[] {1});
    var tenant = java.util.UUID.randomUUID();
    var conversation = java.util.UUID.randomUUID();
    when(conversations.get(new GetConversationQuery(tenant, conversation)))
        .thenReturn(Optional.of(ownedConversation(tenant, conversation, jwt)));
    var controller = controller(storage, quote, metadata, conversations, authorizedResolver());
    var authentication = clientAuthentication(jwt);

    TenantContextHolder.withTenantAndCorrelation(
        tenant,
        "trace",
        () -> controller.submit(image, conversation, "base", null, "idem", jwt, authentication));

    var order = inOrder(quote, metadata);
    order.verify(quote).initialize(any());
    order.verify(metadata).save(any(), any(), any(), any(), anyLong());
    order.verify(quote).process(any());
  }

  @Test
  void removesMetadataAndStorageWhenQuoteProcessingFails() throws Exception {
    var storage = mock(TenantImageWriter.class);
    when(storage.store(any(), any())).thenReturn("tenant/image.img");
    var metadata = mock(DesignImageMetadataRepository.class);
    var quote = mock(ProcessDesignQuoteUseCase.class);
    var conversations = mock(GetConversationUseCase.class);
    when(quote.process(any())).thenThrow(new IllegalStateException("downstream failed"));
    var jwt = jwt();
    var image = new MockMultipartFile("image", "design.jpg", "image/jpeg", new byte[] {1});
    var tenant = java.util.UUID.randomUUID();
    var conversation = java.util.UUID.randomUUID();
    when(conversations.get(new GetConversationQuery(tenant, conversation)))
        .thenReturn(Optional.of(ownedConversation(tenant, conversation, jwt)));
    var controller = controller(storage, quote, metadata, conversations, authorizedResolver());
    var authentication = clientAuthentication(jwt);

    TenantContextHolder.withTenantAndCorrelation(
        tenant,
        "trace",
        () ->
            assertThatThrownBy(
                    () ->
                        controller.submit(
                            image, conversation, "base", null, "idem", jwt, authentication))
                .isInstanceOf(IllegalStateException.class));

    var workflow =
        java.util.UUID.nameUUIDFromBytes(
            ("emme-ai-conversation-workflow-v1:" + conversation + ":idem")
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    verify(metadata).delete(tenant, workflow, "tenant/image.img");
    verify(storage).delete(tenant, "tenant/image.img");
  }

  @Test
  void deletesStoredImageWhenReadingUploadBytesFails() throws Exception {
    var storage = mock(TenantImageWriter.class);
    when(storage.store(any(), any())).thenReturn("tenant/image.img");
    var conversations = mock(GetConversationUseCase.class);
    var controller =
        controller(
            storage,
            mock(ProcessDesignQuoteUseCase.class),
            mock(DesignImageMetadataRepository.class),
            conversations,
            authorizedResolver());
    var image = mock(org.springframework.web.multipart.MultipartFile.class);
    when(image.isEmpty()).thenReturn(false);
    when(image.getSize()).thenReturn(1L);
    when(image.getContentType()).thenReturn("image/jpeg");
    when(image.getBytes())
        .thenReturn(new byte[] {1})
        .thenThrow(new java.io.IOException("read failed"));
    var tenant = java.util.UUID.randomUUID();
    var jwt = jwt();
    var authentication = clientAuthentication(jwt);
    var conversation = java.util.UUID.randomUUID();
    when(conversations.get(new GetConversationQuery(tenant, conversation)))
        .thenReturn(Optional.of(ownedConversation(tenant, conversation, jwt)));
    TenantContextHolder.withTenantAndCorrelation(
        tenant,
        "trace",
        () ->
            assertThatThrownBy(
                    () ->
                        controller.submit(
                            image,
                            conversation,
                            "base",
                            null,
                            "idem",
                            jwt,
                            authentication))
                .isInstanceOf(java.io.UncheckedIOException.class)
                .hasCauseInstanceOf(java.io.IOException.class));
    verify(storage).delete(tenant, "tenant/image.img");
  }

  @Test
  void rejectsNonImageUploadBeforeStorageOrModelExecution() {
    var controller =
        controller(
            mock(TenantImageWriter.class),
            mock(ProcessDesignQuoteUseCase.class),
            mock(DesignImageMetadataRepository.class),
            mock(GetConversationUseCase.class),
            authorizedResolver());
    var file = new MockMultipartFile("image", "design.txt", "text/plain", new byte[] {1});

    assertThatThrownBy(
            () ->
                controller.submit(
                    file, java.util.UUID.randomUUID(), "base", null, "idem", null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("image content type is not supported");
  }

  @Test
  void rejectsAQuoteForAConversationOwnedByAnotherPrincipalBeforeStorage() {
    var storage = mock(TenantImageWriter.class);
    var quote = mock(ProcessDesignQuoteUseCase.class);
    var metadata = mock(DesignImageMetadataRepository.class);
    var conversations = mock(GetConversationUseCase.class);
    var jwt = jwt();
    var tenant = UUID.randomUUID();
    var conversation = UUID.randomUUID();
    when(conversations.get(new GetConversationQuery(tenant, conversation)))
        .thenReturn(Optional.of(new ConversationDetails(
            conversation,
            tenant,
            UUID.randomUUID(),
            ChannelType.WEB_CHAT,
            ConversationStatus.ACTIVE,
            Instant.now())));
    var controller = controller(storage, quote, metadata, conversations, authorizedResolver());

    assertThatThrownBy(
            () ->
                submit(
                    controller,
                    tenant,
                    conversation,
                    jwt,
                    clientAuthentication(jwt)))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessage("Conversation access denied");

    verifyNoInteractions(storage, quote, metadata);
  }

  @Test
  void rejectsAConversationThatIsNotVisibleInTheAuthenticatedTenant() {
    var storage = mock(TenantImageWriter.class);
    var quote = mock(ProcessDesignQuoteUseCase.class);
    var metadata = mock(DesignImageMetadataRepository.class);
    var conversations = mock(GetConversationUseCase.class);
    var jwt = jwt();
    var tenant = UUID.randomUUID();
    var conversation = UUID.randomUUID();
    when(conversations.get(new GetConversationQuery(tenant, conversation)))
        .thenReturn(Optional.empty());
    var controller = controller(storage, quote, metadata, conversations, authorizedResolver());

    assertThatThrownBy(
            () ->
                submit(
                    controller,
                    tenant,
                    conversation,
                    jwt,
                    clientAuthentication(jwt)))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessage("Conversation access denied");

    verify(conversations).get(new GetConversationQuery(tenant, conversation));
    verifyNoInteractions(storage, quote, metadata);
  }

  @Test
  void rejectsAQuoteWhenTheTenantLacksTheAiBasicCapability() {
    var storage = mock(TenantImageWriter.class);
    var quote = mock(ProcessDesignQuoteUseCase.class);
    var metadata = mock(DesignImageMetadataRepository.class);
    var conversations = mock(GetConversationUseCase.class);
    var controller =
        controller(
            storage,
            quote,
            metadata,
            conversations,
            resolver(Set.of("ROLE_tenant_client"), Set.of(), Set.of("ai_chat")));
    var jwt = jwt();

    assertThatThrownBy(
            () ->
                submit(
                    controller,
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    jwt,
                    clientAuthentication(jwt)))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessage("Design quote capability is not enabled");

    verifyNoInteractions(conversations, storage, quote, metadata);
  }

  @Test
  void rejectsAQuoteWhenThePrincipalLacksTheClientRole() {
    var storage = mock(TenantImageWriter.class);
    var quote = mock(ProcessDesignQuoteUseCase.class);
    var metadata = mock(DesignImageMetadataRepository.class);
    var conversations = mock(GetConversationUseCase.class);
    var controller =
        controller(
            storage,
            quote,
            metadata,
            conversations,
            resolver(Set.of("ROLE_tenant_staff"), Set.of("ai:basic"), Set.of("ai_chat")));
    var jwt = jwt();

    assertThatThrownBy(
            () ->
                submit(
                    controller,
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    jwt,
                    clientAuthentication(jwt)))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessage("Design quote client role is required");

    verifyNoInteractions(conversations, storage, quote, metadata);
  }

  private static DesignQuoteController controller(
      TenantImageWriter storage,
      ProcessDesignQuoteUseCase quote,
      DesignImageMetadataRepository metadata,
      GetConversationUseCase conversations,
      AiAuthorizationContextResolver resolver) {
    return new DesignQuoteController(
        storage,
        quote,
        new AiWebExecutionContextFactory(resolver),
        metadata,
        conversations);
  }

  private static AiAuthorizationContextResolver authorizedResolver() {
    return resolver(Set.of("ROLE_tenant_client"), Set.of("ai:basic"), Set.of("ai_chat"));
  }

  private static AiAuthorizationContextResolver resolver(
      Set<String> roles, Set<String> capabilities, Set<String> features) {
    return (tenantId, subject, authenticatedRoles, channel) ->
        new AiAuthorizationContextResolver.AiAuthorizationContext(
            roles, capabilities, features);
  }

  private static void submit(
      DesignQuoteController controller,
      UUID tenant,
      UUID conversation,
      Jwt jwt,
      Authentication authentication)
      throws Exception {
    TenantContextHolder.withTenantAndCorrelation(
        tenant,
        "trace-security",
        () ->
            controller.submit(
                new MockMultipartFile("image", "design.jpg", "image/jpeg", new byte[] {1}),
                conversation,
                "base",
                null,
                "idem-security",
                jwt,
                authentication));
  }

  private static ConversationDetails ownedConversation(
      UUID tenant, UUID conversation, Jwt jwt) {
    return new ConversationDetails(
        conversation,
        tenant,
        AiPrincipalIdentity.fromTrustedClaims(jwt.getIssuer().toString(), jwt.getSubject()),
        ChannelType.WEB_CHAT,
        ConversationStatus.ACTIVE,
        Instant.now());
  }

  private static Jwt jwt() {
    return Jwt.withTokenValue("token")
        .issuer("https://issuer.example/realms/emme")
        .subject("auth0|client-123")
        .header("alg", "none")
        .build();
  }

  private static Authentication clientAuthentication(Jwt jwt) {
    return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
        jwt, null, List.of(new SimpleGrantedAuthority("ROLE_tenant_client")));
  }
}
