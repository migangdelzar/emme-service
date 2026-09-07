package com.emme.assistant.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.TestApplication;
import com.emme.assistant.application.port.out.ConversationRepository;
import com.emme.assistant.application.port.out.WhatsAppWebhookEventRepository;
import com.emme.assistant.domain.model.Conversation;
import com.emme.kernel.context.TenantContextHolder;
import com.emme.kernel.type.ChannelType;
import com.emme.tenancy.adapter.out.client.database.TenantSchemaName;
import com.emme.tenancy.application.port.out.TenantProvisioningRepository;
import com.emme.tenancy.application.port.out.TenantSchemaMigrationPort;
import com.emme.testing.integration.annotation.PostgresIntegrationTest;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(classes = TestApplication.class)
@PostgresIntegrationTest
@DisplayName("assistant integration test")
class AssistantIntegrationTest {

  @Autowired private DataSource dataSource;

  @Autowired private WhatsAppWebhookEventRepository webhookEvents;
  @Autowired private ConversationRepository conversations;
  @Autowired private TenantProvisioningRepository provisioningRepository;
  @Autowired private TenantSchemaMigrationPort schemaMigrationPort;
  @Autowired private PlatformTransactionManager transactionManager;

  @Autowired
  @Qualifier("bootstrapJdbcClient")
  private JdbcClient bootstrapJdbc;

  @BeforeEach
  void installDatabaseExtensions() {
    bootstrapJdbc.sql("CREATE EXTENSION IF NOT EXISTS vector").update();
  }

  @Test
  @DisplayName("PostgreSQL container wired via @ServiceConnection")
  void postgresWired() throws Exception {
    try (var conn = dataSource.getConnection()) {
      var stmt = conn.createStatement();
      var rs = stmt.executeQuery("SELECT 1");
      assertThat(rs.next()).isTrue();
    }
  }

  @Test
  @DisplayName("Spring context boots")
  void contextLoads() {
    assertThat(dataSource).isNotNull();
  }

  @Test
  @DisplayName("PostgreSQL claim is tenant-scoped and replay-safe")
  void webhookClaimIsTenantScopedAndReplaySafe() {
    UUID tenantId = UUID.randomUUID();
    UUID otherTenantId = UUID.randomUUID();

    assertThat(webhookEvents.claim(tenantId, "whatsapp", "wamid-integration-1")).isTrue();
    assertThat(webhookEvents.claim(tenantId, "whatsapp", "wamid-integration-1")).isFalse();
    assertThat(webhookEvents.claim(otherTenantId, "whatsapp", "wamid-integration-1")).isTrue();
  }

  @Test
  @DisplayName("JPA conversation persistence uses the tenant selected schema")
  void conversationCrudUsesTheTenantSelectedSchema() {
    UUID tenantA = provisionTenant("conversation-a");
    UUID tenantB = provisionTenant("conversation-b");
    UUID participantId = UUID.randomUUID();
    Conversation saved =
        TenantContextHolder.withTenantOverride(
            tenantA,
            () ->
                conversations.save(new Conversation(tenantA, participantId, ChannelType.WEB_CHAT)));

    TenantContextHolder.withTenantOverride(
        tenantB, () -> assertThat(conversations.findById(saved.id())).isEmpty());
    TenantContextHolder.withTenantOverride(
        tenantA,
        () ->
            assertThat(conversations.findById(saved.id()))
                .get()
                .extracting(Conversation::tenantId)
                .isEqualTo(tenantA));
  }

  @Test
  @DisplayName("JPA conversation updates reject a stale version")
  void concurrentConversationUpdatesHaveOneOptimisticLockWinner() throws Exception {
    UUID tenantId = provisionTenant("conversation-lock");
    UUID conversationId =
        TenantContextHolder.withTenantOverride(
            tenantId,
            () ->
                new TransactionTemplate(transactionManager)
                    .execute(
                        status ->
                            conversations
                                .save(
                                    new Conversation(
                                        tenantId, UUID.randomUUID(), ChannelType.WEB_CHAT))
                                .id()));
    CountDownLatch bothLoaded = new CountDownLatch(2);
    CountDownLatch releaseWrites = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);

    try {
      Future<Throwable> first =
          executor.submit(
              () -> updateConversation(tenantId, conversationId, bothLoaded, releaseWrites));
      Future<Throwable> second =
          executor.submit(
              () -> updateConversation(tenantId, conversationId, bothLoaded, releaseWrites));

      assertThat(bothLoaded.await(10, TimeUnit.SECONDS)).isTrue();
      releaseWrites.countDown();

      Throwable firstFailure = first.get(10, TimeUnit.SECONDS);
      Throwable secondFailure = second.get(10, TimeUnit.SECONDS);
      assertThat(firstFailure == null ^ secondFailure == null).isTrue();
      assertThat(firstFailure == null ? secondFailure : firstFailure)
          .isInstanceOf(OptimisticLockingFailureException.class);
    } finally {
      releaseWrites.countDown();
      executor.shutdownNow();
    }
  }

  private Throwable updateConversation(
      UUID tenantId, UUID conversationId, CountDownLatch bothLoaded, CountDownLatch releaseWrites) {
    try {
      TenantContextHolder.withTenantOverride(
          tenantId,
          () ->
              new TransactionTemplate(transactionManager)
                  .executeWithoutResult(
                      status -> {
                        Conversation conversation =
                            conversations.findById(conversationId).orElseThrow();
                        bothLoaded.countDown();
                        await(releaseWrites);
                        conversation.close();
                        conversations.save(conversation);
                      }));
      return null;
    } catch (Throwable failure) {
      return failure;
    }
  }

  private static void await(CountDownLatch latch) {
    try {
      if (!latch.await(10, TimeUnit.SECONDS)) {
        throw new IllegalStateException("Timed out waiting for concurrent conversation updates");
      }
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(
          "Interrupted while coordinating concurrent conversation updates", interrupted);
    }
  }

  private UUID provisionTenant(String prefix) {
    UUID tenantId = UUID.randomUUID();
    String slug = prefix + "-" + tenantId.toString().replace('-', 'a');
    String schemaName = TenantSchemaName.fromSlug(slug);
    provisioningRepository.requestProvisioning(tenantId, slug, schemaName);
    assertThat(schemaMigrationPort.migrate(tenantId, slug)).isEqualTo(schemaName);
    return tenantId;
  }
}
