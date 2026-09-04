package com.emme.assistant.adapter.in.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.assistant.adapter.in.web.request.StartConversationRequest;
import com.emme.assistant.ai.adapter.in.web.security.AiPrincipalIdentity;
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
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class ConversationControllerTest {

  @Test
  void derivesWebConversationOwnershipFromTheAuthenticatedIdentity() {
    var start = mock(StartConversationUseCase.class);
    var tenant = UUID.randomUUID();
    var conversation = UUID.randomUUID();
    var jwt = jwt();
    var controller = controller(start);
    var details =
        new ConversationDetails(
            conversation,
            tenant,
            AiPrincipalIdentity.fromTrustedClaims(jwt.getIssuer().toString(), jwt.getSubject()),
            ChannelType.WEB_CHAT,
            ConversationStatus.ACTIVE,
            Instant.now());
    when(start.start(any(StartConversationCommand.class))).thenReturn(details);

    TenantContextHolder.withTenantOverride(
        tenant,
        () ->
            controller.start(
                new StartConversationRequest(UUID.randomUUID(), ChannelType.WEB_CHAT), jwt));

    verify(start)
        .start(
            new StartConversationCommand(
                tenant,
                AiPrincipalIdentity.fromTrustedClaims(jwt.getIssuer().toString(), jwt.getSubject()),
                ChannelType.WEB_CHAT));
  }

  private static ConversationController controller(StartConversationUseCase start) {
    return new ConversationController(
        start,
        mock(ListConversationsUseCase.class),
        mock(GetConversationUseCase.class),
        mock(CloseConversationUseCase.class),
        mock(GetConversationHistoryUseCase.class),
        mock(ProposePendingActionUseCase.class),
        mock(ConfirmPendingActionUseCase.class),
        mock(RejectPendingActionUseCase.class));
  }

  private static Jwt jwt() {
    return Jwt.withTokenValue("token")
        .issuer("https://issuer.example/realms/emme")
        .subject("auth0|client-123")
        .header("alg", "none")
        .build();
  }
}
