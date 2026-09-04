package com.emme.assistant.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.emme.assistant.api.command.ProcessWhatsAppMessageCommand;
import com.emme.assistant.api.event.WhatsAppMessageReceived;
import com.emme.assistant.api.usecase.ProcessWhatsAppMessageUseCase;
import com.emme.kernel.context.TenantContextHolder;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WhatsAppMessageReceivedListenerTest {

  @Test
  void restoresEventTenantAndDatabaseBeforeDelegatingToTheUseCase() {
    ProcessWhatsAppMessageUseCase useCase = org.mockito.Mockito.mock();
    WhatsAppMessageReceivedListener listener = new WhatsAppMessageReceivedListener(useCase);
    UUID tenantId = UUID.randomUUID();
    UUID databaseId = UUID.randomUUID();
    ProcessWhatsAppMessageCommand command =
        new ProcessWhatsAppMessageCommand(tenantId, "event-1", "phone", "hello");

    listener.onMessage(new WhatsAppMessageReceived(command, databaseId));

    verify(useCase).process(command);
    assertThat(TenantContextHolder.currentTenantOptional()).isEmpty();
    assertThat(TenantContextHolder.currentDatabaseOptional()).isEmpty();
  }
}
