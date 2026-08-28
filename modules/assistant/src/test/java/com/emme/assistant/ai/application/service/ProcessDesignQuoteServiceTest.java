package com.emme.assistant.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.assistant.ai.api.command.ProcessDesignQuoteCommand;
import com.emme.assistant.ai.api.result.QuoteWorkflowResult;
import com.emme.assistant.ai.application.port.out.NailDesignExtractionRejectedException;
import com.emme.assistant.ai.application.port.out.NailDesignExtractor;
import com.emme.assistant.ai.application.port.out.QuoteArtifactRepository;
import com.emme.assistant.ai.application.port.out.QuoteWorkflowRepository;
import com.emme.assistant.ai.domain.quote.ArtComplexity;
import com.emme.assistant.ai.domain.quote.DeterministicQuoteCalculator;
import com.emme.assistant.ai.domain.quote.ExtensionType;
import com.emme.assistant.ai.domain.quote.NailDesignFeatures;
import com.emme.assistant.ai.domain.quote.NailLength;
import com.emme.assistant.ai.domain.quote.NailShape;
import com.emme.assistant.ai.domain.quote.QuoteFeatureCondition;
import com.emme.assistant.ai.domain.quote.QuoteLineType;
import com.emme.assistant.ai.domain.quote.QuoteTemplate;
import com.emme.assistant.ai.domain.quote.QuoteTemplateLine;
import com.emme.assistant.ai.domain.workflow.QuoteDraft;
import com.emme.assistant.ai.domain.workflow.QuoteReviewTask;
import com.emme.assistant.ai.domain.workflow.QuoteWorkflow;
import com.emme.assistant.ai.domain.workflow.QuoteWorkflowState;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProcessDesignQuoteServiceTest {

  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final UUID PRINCIPAL_ID = UUID.randomUUID();
  private static final UUID CONVERSATION_ID = UUID.randomUUID();
  private static final UUID WORKFLOW_ID = UUID.randomUUID();

  @Test
  void returnsAReadyDeterministicQuoteWhenExtractionIsSafe() {
    RecordingExtractor extractor = new RecordingExtractor(features(false, List.of()));
    RecordingArtifacts artifacts = new RecordingArtifacts();
    ProcessDesignQuoteService service = service(extractor, artifacts);

    QuoteWorkflowResult result =
        run(
            service,
            new ProcessDesignQuoteCommand("base", "Please quote this design", "images/design-1"));

    assertThat(result.state()).isEqualTo(QuoteWorkflowState.QUOTE_READY);
    assertThat(result.quote()).isPresent();
    assertThat(result.quote().orElseThrow().minimumPrice()).isEqualByComparingTo("380.00");
    assertThat(result.reviewTask()).isEmpty();
    assertThat(artifacts.draft()).isNotNull();
    assertThat(artifacts.reviewTask()).isNull();
    assertThat(extractor.calls()).isEqualTo(1);
  }

  @Test
  void persistsAReviewTaskAndDoesNotReturnAClientReadyQuoteWhenExtractionIsAmbiguous() {
    RecordingArtifacts artifacts = new RecordingArtifacts();
    ProcessDesignQuoteService service =
        service(new RecordingExtractor(features(true, List.of("image is unclear"))), artifacts);

    QuoteWorkflowResult result =
        run(
            service,
            new ProcessDesignQuoteCommand("base", "Can you price this?", "images/unclear"));

    assertThat(result.state()).isEqualTo(QuoteWorkflowState.WAITING_FOR_STAFF);
    assertThat(result.quote()).isPresent();
    assertThat(result.reviewTask()).isPresent();
    assertThat(result.reviewTask().orElseThrow().status().name()).isEqualTo("WAITING_FOR_STAFF");
    assertThat(artifacts.reviewTask()).isEqualTo(result.reviewTask().orElseThrow());
  }

  @Test
  void returnsTheExistingWorkflowForTheSameBackendIdempotencyKey() {
    RecordingWorkflowRepository workflows = new RecordingWorkflowRepository();
    QuoteWorkflow existing =
        QuoteWorkflow.received(WORKFLOW_ID, TENANT_ID, PRINCIPAL_ID, CONVERSATION_ID, "idem-1")
            .transition(0, QuoteWorkflowState.EXTRACTING);
    workflows.saved = existing;
    RecordingExtractor extractor = new RecordingExtractor(features(false, List.of()));
    ProcessDesignQuoteService service = service(workflows, extractor, new RecordingArtifacts());

    QuoteWorkflowResult result =
        run(service, new ProcessDesignQuoteCommand("base", "retry", "images/design-1"));

    assertThat(result.workflowId()).isEqualTo(WORKFLOW_ID);
    assertThat(result.state()).isEqualTo(QuoteWorkflowState.EXTRACTING);
    assertThat(extractor.calls()).isZero();
  }

  @Test
  void marksTheWorkflowFailedWhenTheExtractorRejectsModelOutput() {
    RecordingExtractor extractor = new RecordingExtractor(features(false, List.of()));
    extractor.rejection = new NailDesignExtractionRejectedException("invalid structured output");
    RecordingWorkflowRepository workflows = new RecordingWorkflowRepository();
    ProcessDesignQuoteService service = service(workflows, extractor, new RecordingArtifacts());

    QuoteWorkflowResult result =
        run(service, new ProcessDesignQuoteCommand("base", "bad extraction", "images/design-2"));

    assertThat(result.state()).isEqualTo(QuoteWorkflowState.FAILED);
    assertThat(workflows.saved.state()).isEqualTo(QuoteWorkflowState.FAILED);
  }

  private static ProcessDesignQuoteService service(
      NailDesignExtractor extractor, RecordingArtifacts artifacts) {
    return service(new RecordingWorkflowRepository(), extractor, artifacts);
  }

  private static ProcessDesignQuoteService service(
      RecordingWorkflowRepository workflows,
      NailDesignExtractor extractor,
      RecordingArtifacts artifacts) {
    return new ProcessDesignQuoteService(
        workflows,
        extractor,
        key -> Optional.of(template()),
        artifacts,
        new DeterministicQuoteCalculator(0.80),
        () -> UUID.randomUUID());
  }

  private static QuoteWorkflowResult run(
      ProcessDesignQuoteService service, ProcessDesignQuoteCommand command) {
    AiExecutionContext context =
        new AiExecutionContext(
            TENANT_ID,
            PRINCIPAL_ID,
            java.util.Set.of("CLIENT"),
            CONVERSATION_ID,
            WORKFLOW_ID,
            "trace-1",
            "idem-1");
    return AiExecutionContextScope.call(context, () -> service.process(command));
  }

  private static QuoteTemplate template() {
    return new QuoteTemplate(
        "base",
        "v1",
        "MXN",
        List.of(
            new QuoteTemplateLine(
                "base",
                QuoteLineType.REQUIRED_SERVICE,
                new BigDecimal("300.00"),
                new BigDecimal("350.00"),
                60,
                QuoteFeatureCondition.always()),
            new QuoteTemplateLine(
                "chrome",
                QuoteLineType.ADD_ON,
                new BigDecimal("80.00"),
                new BigDecimal("100.00"),
                15,
                QuoteFeatureCondition.forEffect(
                    com.emme.assistant.ai.domain.quote.NailEffect.CHROME))));
  }

  private static NailDesignFeatures features(boolean review, List<String> ambiguities) {
    return new NailDesignFeatures(
        NailShape.ALMOND,
        NailLength.MEDIUM,
        "pink",
        List.of(com.emme.assistant.ai.domain.quote.NailEffect.CHROME),
        List.of(),
        ExtensionType.NONE,
        false,
        false,
        ArtComplexity.MODERATE,
        Map.of("shape", 0.99, "artComplexity", 0.95),
        ambiguities,
        review);
  }

  private static final class RecordingExtractor implements NailDesignExtractor {
    private final NailDesignFeatures features;
    private int calls;
    private NailDesignExtractionRejectedException rejection;

    private RecordingExtractor(NailDesignFeatures features) {
      this.features = features;
    }

    @Override
    public ExtractionResult extract(ExtractionRequest request) {
      calls++;
      if (rejection != null) {
        throw rejection;
      }
      return new ExtractionResult(features, "vision-v1", "quote-prompt-v1", "nail-features-v1");
    }

    private int calls() {
      return calls;
    }
  }

  private static final class RecordingWorkflowRepository implements QuoteWorkflowRepository {
    private QuoteWorkflow saved;

    @Override
    public Optional<QuoteWorkflow> findByIdempotencyKey(String idempotencyKey) {
      return Optional.ofNullable(saved);
    }

    @Override
    public Optional<QuoteWorkflow> findById(UUID workflowId) {
      return saved != null && saved.id().equals(workflowId) ? Optional.of(saved) : Optional.empty();
    }

    @Override
    public QuoteWorkflow save(QuoteWorkflow workflow) {
      saved = workflow;
      return workflow;
    }
  }

  private static final class RecordingArtifacts implements QuoteArtifactRepository {
    private QuoteDraft draft;
    private QuoteReviewTask reviewTask;

    @Override
    public void saveExtraction(UUID workflowId, NailDesignExtractor.ExtractionResult extraction) {}

    @Override
    public void saveDraft(QuoteDraft draft) {
      this.draft = draft;
    }

    @Override
    public QuoteReviewTask saveReviewTask(QuoteReviewTask reviewTask) {
      this.reviewTask = reviewTask;
      return reviewTask;
    }

    private QuoteDraft draft() {
      return draft;
    }

    private QuoteReviewTask reviewTask() {
      return reviewTask;
    }
  }
}
