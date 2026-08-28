package com.emme.assistant.ai.application.service;

import com.emme.assistant.ai.api.command.ProcessDesignQuoteCommand;
import com.emme.assistant.ai.api.result.QuoteWorkflowResult;
import com.emme.assistant.ai.application.port.out.AiWorkflowObserver;
import com.emme.assistant.ai.application.port.out.NailDesignExtractionRejectedException;
import com.emme.assistant.ai.application.port.out.NailDesignExtractor;
import com.emme.assistant.ai.application.port.out.QuoteArtifactRepository;
import com.emme.assistant.ai.application.port.out.QuoteTemplateRepository;
import com.emme.assistant.ai.application.port.out.QuoteWorkflowRepository;
import com.emme.assistant.ai.domain.quote.DeterministicQuoteCalculator;
import com.emme.assistant.ai.domain.quote.QuoteCalculation;
import com.emme.assistant.ai.domain.workflow.QuoteDraft;
import com.emme.assistant.ai.domain.workflow.QuoteReviewTask;
import com.emme.assistant.ai.domain.workflow.QuoteWorkflow;
import com.emme.assistant.ai.domain.workflow.QuoteWorkflowState;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates extraction, deterministic calculation, and the persisted HITL pause. */
@org.springframework.stereotype.Service
@Transactional
@ConditionalOnProperty(prefix = "app.ai.quote", name = "enabled", havingValue = "true")
public class ProcessDesignQuoteService
    implements com.emme.assistant.ai.api.usecase.ProcessDesignQuoteUseCase {

  private final QuoteWorkflowRepository workflows;
  private final NailDesignExtractor extractor;
  private final QuoteTemplateRepository templates;
  private final QuoteArtifactRepository artifacts;
  private final DeterministicQuoteCalculator calculator;
  private final Supplier<UUID> idGenerator;
  private final AiWorkflowObserver observer;

  public ProcessDesignQuoteService(
      QuoteWorkflowRepository workflows,
      NailDesignExtractor extractor,
      QuoteTemplateRepository templates,
      QuoteArtifactRepository artifacts,
      DeterministicQuoteCalculator calculator,
      Supplier<UUID> idGenerator,
      AiWorkflowObserver observer) {
    this.workflows = Objects.requireNonNull(workflows, "workflows must not be null");
    this.extractor = Objects.requireNonNull(extractor, "extractor must not be null");
    this.templates = Objects.requireNonNull(templates, "templates must not be null");
    this.artifacts = Objects.requireNonNull(artifacts, "artifacts must not be null");
    this.calculator = Objects.requireNonNull(calculator, "calculator must not be null");
    this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
    this.observer = Objects.requireNonNull(observer, "observer must not be null");
  }

  @Override
  public QuoteWorkflowResult process(ProcessDesignQuoteCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    Instant startedAt = Instant.now();
    observer.workflowStarted("DESIGN_QUOTE");
    try {
      QuoteWorkflowResult result = processInternal(command);
      observer.workflowFinished(
          "DESIGN_QUOTE", result.state().name(), Duration.between(startedAt, Instant.now()));
      return result;
    } catch (RuntimeException | Error exception) {
      observer.workflowFinished(
          "DESIGN_QUOTE", "FAILED", Duration.between(startedAt, Instant.now()));
      throw exception;
    }
  }

  private QuoteWorkflowResult processInternal(ProcessDesignQuoteCommand command) {
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();
    Optional<QuoteWorkflow> existing = workflows.findByIdempotencyKey(context.idempotencyKey());
    if (existing.isPresent()) {
      return result(existing.orElseThrow(), Optional.empty(), Optional.empty());
    }

    QuoteWorkflow workflow =
        QuoteWorkflow.received(
            context.workflowId(),
            context.tenantId(),
            context.principalId(),
            context.conversationId(),
            context.idempotencyKey());
    workflow = workflows.save(workflow);

    workflow = transitionAndSave(workflow, QuoteWorkflowState.EXTRACTING);
    NailDesignExtractor.ExtractionResult extraction;
    try {
      extraction =
          extractor.extract(
              new NailDesignExtractor.ExtractionRequest(
                  command.inputText(), command.imageStorageKey()));
    } catch (NailDesignExtractionRejectedException exception) {
      QuoteWorkflow failed = transitionAndSave(workflow, QuoteWorkflowState.FAILED);
      return result(failed, Optional.empty(), Optional.empty());
    }
    artifacts.saveExtraction(workflow.id(), extraction);

    QuoteCalculation calculation =
        calculator.calculate(
            extraction.features(),
            templates
                .findByKey(command.templateKey())
                .orElseThrow(
                    () ->
                        new IllegalArgumentException(
                            "Quote template not found: " + command.templateKey())));
    artifacts.saveDraft(
        QuoteDraft.create(idGenerator.get(), context.tenantId(), workflow.id(), calculation));

    workflow = transitionAndSave(workflow, QuoteWorkflowState.QUOTE_CALCULATED);
    if (!calculation.needsHumanReview()) {
      workflow = transitionAndSave(workflow, QuoteWorkflowState.QUOTE_READY);
      return result(workflow, Optional.of(calculation), Optional.empty());
    }

    workflow = transitionAndSave(workflow, QuoteWorkflowState.NEEDS_STAFF_REVIEW);
    QuoteReviewTask reviewTask =
        artifacts.saveReviewTask(
            QuoteReviewTask.waiting(
                idGenerator.get(), context.tenantId(), workflow.id(), calculation.reviewReasons()));
    workflow = transitionAndSave(workflow, QuoteWorkflowState.WAITING_FOR_STAFF);
    return result(workflow, Optional.of(calculation), Optional.of(reviewTask));
  }

  private QuoteWorkflow transitionAndSave(QuoteWorkflow workflow, QuoteWorkflowState state) {
    return workflows.save(workflow.transition(workflow.version(), state));
  }

  private QuoteWorkflowResult result(
      QuoteWorkflow workflow,
      Optional<QuoteCalculation> calculation,
      Optional<QuoteReviewTask> reviewTask) {
    return new QuoteWorkflowResult(workflow.id(), workflow.state(), calculation, reviewTask);
  }
}
