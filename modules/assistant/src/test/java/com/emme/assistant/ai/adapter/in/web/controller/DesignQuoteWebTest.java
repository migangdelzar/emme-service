package com.emme.assistant.ai.adapter.in.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emme.ai.contracts.image.TenantImageWriter;
import com.emme.ai.contracts.tenant.AiAuthorizationContextResolver;
import com.emme.assistant.adapter.in.web.request.StartConversationRequest;
import com.emme.assistant.ai.adapter.in.web.security.AiPrincipalIdentity;
import com.emme.assistant.ai.adapter.in.web.security.AiWebExecutionContextFactory;
import com.emme.assistant.ai.api.result.QuoteWorkflowResult;
import com.emme.assistant.ai.api.usecase.ProcessDesignQuoteUseCase;
import com.emme.assistant.ai.application.port.out.DesignImageMetadataRepository;
import com.emme.assistant.ai.domain.workflow.QuoteWorkflowState;
import com.emme.assistant.api.command.StartConversationCommand;
import com.emme.assistant.api.result.ConversationDetails;
import com.emme.assistant.api.type.ConversationStatus;
import com.emme.assistant.api.usecase.CloseConversationUseCase;
import com.emme.assistant.api.usecase.ConfirmPendingActionUseCase;
import com.emme.assistant.api.usecase.GetConversationHistoryUseCase;
import com.emme.assistant.api.usecase.GetConversationUseCase;
import com.emme.assistant.api.usecase.ListConversationsUseCase;
import com.emme.assistant.api.usecase.ProposePendingActionUseCase;
import com.emme.assistant.api.usecase.RejectPendingActionUseCase;
import com.emme.assistant.api.usecase.StartConversationUseCase;
import com.emme.kernel.context.TenantContextHolder;
import com.emme.kernel.type.ChannelType;
import com.emme.shared.web.advice.GlobalExceptionHandler;
import com.emme.shared.web.i18n.MessageResolver;
import com.emme.shared.web.i18n.ProblemDetailFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;
import org.springframework.web.accept.ApiVersionStrategy;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.multipart.MultipartFile;

@SuppressWarnings("deprecation")
class DesignQuoteWebTest {

  private static final String ISSUER = "https://issuer.example/realms/emme";
  private static final String OWNER_SUBJECT = "owner-subject";
  private static final String OTHER_SUBJECT = "other-subject";

  private AnnotationConfigApplicationContext context;
  private MockMvc mockMvc;
  private QuoteTestState state;

  @BeforeEach
  void setUp() {
    state = new QuoteTestState();
    context = new AnnotationConfigApplicationContext();
    context.registerBean(QuoteTestState.class, () -> state);
    context.register(QuoteTestConfiguration.class);
    context.refresh();
    var source = new StaticMessageSource();
    mockMvc =
        MockMvcBuilders.standaloneSetup(
                context.getBean(DesignQuoteController.class))
            .setControllerAdvice(
                new GlobalExceptionHandler(new ProblemDetailFactory(new MessageResolver(source))))
            .setCustomArgumentResolvers(
                new AuthenticationPrincipalArgumentResolver(),
                new ImageArgumentResolver())
            .setApiVersionStrategy(apiVersionStrategy())
            .addFilters(
                new SecurityContextPersistenceFilter(new HttpSessionSecurityContextRepository()))
            .build();
    assertThat(AopUtils.isAopProxy(context.getBean(DesignQuoteController.class))).isTrue();
  }

  @AfterEach
  void tearDown() {
    if (context != null) context.close();
  }

  @Test
  void createsSupportedConversationThenAllowsTheOwnerToUploadAQuote() throws Exception {
    UUID conversationId = createConversation(OWNER_SUBJECT);

    upload(conversationId, OWNER_SUBJECT).andExpect(status().isAccepted());
  }

  @Test
  void deniesUploadByAnotherPrincipalInTheSameTenant() throws Exception {
    UUID conversationId = createConversation(OWNER_SUBJECT);

    upload(conversationId, OTHER_SUBJECT).andExpect(status().isForbidden());
  }

  @Test
  void deniesUploadFromAnotherTenant() throws Exception {
    UUID conversationId = createConversation(OWNER_SUBJECT);
    UUID otherTenant = UUID.randomUUID();

    upload(conversationId, OWNER_SUBJECT, otherTenant).andExpect(status().isForbidden());
  }

  @Test
  void rejectsQuoteUploadAtTheMethodSecurityProxyForNonClientRole() throws Exception {
    state.featureFlag.enabled = true;
    mockMvc
        .perform(uploadRequest(UUID.randomUUID(), UUID.randomUUID(), OWNER_SUBJECT, "tenant_staff"))
        .andExpect(status().isForbidden());

    verifyNoInteractions(state.quote, state.storage, state.metadata);
  }

  @Test
  void rejectsQuoteUploadAtTheMethodSecurityProxyWhenFeatureIsDisabled() throws Exception {
    state.featureFlag.enabled = false;
    mockMvc
        .perform(uploadRequest(UUID.randomUUID(), UUID.randomUUID(), OWNER_SUBJECT, "client"))
        .andExpect(status().isForbidden());

    verifyNoInteractions(state.quote, state.storage, state.metadata);
  }

  @Test
  void rejectsQuoteUploadWhenTenantLacksTheAiBasicCapability() throws Exception {
    state.capabilities = Set.of();
    UUID conversationId = createConversation(OWNER_SUBJECT);

    upload(conversationId, OWNER_SUBJECT).andExpect(status().isForbidden());

    verifyNoInteractions(state.quote, state.storage, state.metadata);
  }

  private UUID createConversation(String subject) throws Exception {
    UUID tenant = UUID.randomUUID();
    var response =
        TenantContextHolder.withTenantAndCorrelation(
            tenant,
            "create-" + subject,
            () ->
                context
                    .getBean(com.emme.assistant.adapter.in.web.controller.ConversationController.class)
                    .start(new StartConversationRequest(UUID.randomUUID(), ChannelType.WEB_CHAT), jwt(subject))
                    .getBody());
    state.tenant = tenant;
    return response.id();
  }

  private org.springframework.test.web.servlet.ResultActions upload(
      UUID conversationId, String subject) throws Exception {
    return upload(conversationId, subject, state.tenant);
  }

  private org.springframework.test.web.servlet.ResultActions upload(
      UUID conversationId, String subject, UUID tenant) throws Exception {
    return TenantContextHolder.withTenantAndCorrelation(
        tenant,
        "upload-" + subject,
        () -> mockMvc.perform(uploadRequest(conversationId, tenant, subject, "client")));
  }

  private MockMultipartHttpServletRequestBuilder uploadRequest(
      UUID conversationId, UUID tenant, String subject, String role) {
    return multipart("/api/ai/quotes")
        .file(new MockMultipartFile("image", "design.jpg", "image/jpeg", new byte[] {1}))
        .with(authentication(jwtAuthentication(subject, role)))
        .with(
            request -> {
              request.setUserPrincipal(jwtAuthentication(subject, role));
              return request;
            })
        .param("conversationId", conversationId.toString())
        .param("templateKey", "base")
        .param("idempotencyKey", "quote-" + subject + "-" + conversationId);
  }

  private static ApiVersionStrategy apiVersionStrategy() {
    return new ApiVersionStrategy() {
      @Override
      public String resolveVersion(jakarta.servlet.http.HttpServletRequest request) {
        return request.getHeader("API-Version");
      }

      @Override
      public Comparable<?> parseVersion(String version) {
        return version;
      }

      @Override
      public void validateVersion(
          Comparable<?> version, jakarta.servlet.http.HttpServletRequest request) {}

      @Override
      public Comparable<?> getDefaultVersion() {
        return "1.0";
      }

      @Override
      public void handleDeprecations(
          Comparable<?> version,
          Object handler,
          jakarta.servlet.http.HttpServletRequest request,
          jakarta.servlet.http.HttpServletResponse response) {}
    };
  }

  private static final class ImageArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
      return parameter.hasParameterAnnotation(RequestPart.class)
          && MultipartFile.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
        MethodParameter parameter,
        ModelAndViewContainer container,
        NativeWebRequest request,
        WebDataBinderFactory binderFactory) {
      return new MockMultipartFile("image", "design.jpg", "image/jpeg", new byte[] {1});
    }
  }

  private static Authentication jwtAuthentication(String subject, String role) {
    return new UsernamePasswordAuthenticationToken(
        jwt(subject), null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
  }

  private static Jwt jwt(String subject) {
    return Jwt.withTokenValue("token")
        .issuer(ISSUER)
        .subject(subject)
        .claim("tenant_id", UUID.randomUUID().toString())
        .header("alg", "none")
        .build();
  }

  @TestConfiguration(proxyBeanMethods = false)
  @EnableMethodSecurity
  static class QuoteTestConfiguration {
    @Bean
    com.emme.assistant.adapter.in.web.controller.ConversationController conversationController(
        StartConversationUseCase start) {
      return new com.emme.assistant.adapter.in.web.controller.ConversationController(
          start,
          mock(ListConversationsUseCase.class),
          mock(GetConversationUseCase.class),
          mock(CloseConversationUseCase.class),
          mock(GetConversationHistoryUseCase.class),
          mock(ProposePendingActionUseCase.class),
          mock(ConfirmPendingActionUseCase.class),
          mock(RejectPendingActionUseCase.class));
    }

    @Bean
    StartConversationUseCase startConversation(QuoteTestState state) {
      return command -> state.start(command);
    }

    @Bean
    GetConversationUseCase getConversation(QuoteTestState state) {
      return query -> state.get(query.tenantId(), query.conversationId());
    }

    @Bean
    DesignQuoteController designQuoteController(
        TenantImageWriter storage,
        ProcessDesignQuoteUseCase quote,
        AiWebExecutionContextFactory contexts,
        DesignImageMetadataRepository metadata,
        GetConversationUseCase conversations) {
      return new DesignQuoteController(storage, quote, contexts, metadata, conversations);
    }

    @Bean
    AiWebExecutionContextFactory aiWebExecutionContextFactory(
        AiAuthorizationContextResolver resolver) {
      return new AiWebExecutionContextFactory(resolver);
    }

    @Bean
    @Primary
    AiAuthorizationContextResolver authorizationResolver(QuoteTestState state) {
      return (tenant, subject, roles, channel) ->
          new AiAuthorizationContextResolver.AiAuthorizationContext(
              Set.copyOf(roles), state.capabilities, Set.of("ai_chat"));
    }

    @Bean
    @Primary
    TenantImageWriter imageWriter(QuoteTestState state) {
      return state.storage;
    }

    @Bean
    @Primary
    ProcessDesignQuoteUseCase quoteUseCase(QuoteTestState state) {
      return state.quote;
    }

    @Bean
    @Primary
    DesignImageMetadataRepository imageMetadata(QuoteTestState state) {
      return state.metadata;
    }

    @Bean(name = "featureFlagService")
    FeatureFlagGate featureFlagService(QuoteTestState state) {
      return state.featureFlag;
    }
  }

  static final class FeatureFlagGate {
    private boolean enabled;

    public boolean isEnabled(String code) {
      return "ai_chat".equals(code) && enabled;
    }
  }

  static final class QuoteTestState {
    private final Map<UUID, ConversationDetails> conversations = new HashMap<>();
    private final TenantImageWriter storage = mock(TenantImageWriter.class);
    private final ProcessDesignQuoteUseCase quote = mock(ProcessDesignQuoteUseCase.class);
    private final DesignImageMetadataRepository metadata = mock(DesignImageMetadataRepository.class);
    private final FeatureFlagGate featureFlag = new FeatureFlagGate();
    private Set<String> capabilities = Set.of("ai:basic");
    private UUID tenant;

    private QuoteTestState() {
      when(storage.store(any(), any())).thenReturn("test/design.jpg");
      when(quote.process(any()))
          .thenReturn(
              new QuoteWorkflowResult(
                  UUID.randomUUID(),
                  QuoteWorkflowState.QUOTE_READY,
                  Optional.empty(),
                  Optional.empty()));
      featureFlag.enabled = true;
    }

    private ConversationDetails start(StartConversationCommand command) {
      var details =
          new ConversationDetails(
              UUID.randomUUID(),
              command.tenantId(),
              command.participantId(),
              command.channel(),
              ConversationStatus.ACTIVE,
              java.time.Instant.now());
      conversations.put(details.id(), details);
      return details;
    }

    private Optional<ConversationDetails> get(UUID tenantId, UUID conversationId) {
      return Optional.ofNullable(conversations.get(conversationId))
          .filter(conversation -> tenantId.equals(conversation.tenantId()));
    }
  }
}
