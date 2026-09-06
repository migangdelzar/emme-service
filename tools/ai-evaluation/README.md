# Emme AI evaluation worker

This is an offline evaluation boundary for anonymized AI traces. It is not an
online agent and it never promotes candidates, writes production routing
indexes, or changes tenant configuration.

The workflow is:

```text
PostgreSQL/exported traces
  -> JSONL dataset
  -> PII redaction
  -> Ragas evaluation
  -> metrics and advisory gates
  -> Java lifecycle validation
  -> separately approved shadow/canary promotion
```

Use Python 3.13 and `uv`:

```bash
cd tools/ai-evaluation
uv sync --dev
uv run pytest
uv run emme-ai-evaluate --dataset datasets/example.jsonl --output evals/report.json
```

Each JSONL row must contain `user_input`, `response`, `retrieved_contexts`,
`accepted: true`, and a successful `outcome` (`success` or `succeeded`).
`reference` is optional. Rows without an accepted retrieval decision or a
successful outcome fail the safety gate and are not sent to the metric engine.
Tenant IDs, principal IDs,
workflow IDs, and raw trace metadata are intentionally not passed to Ragas.

The report's `regression_passed` and `shadow_comparison_passed` fields are
advisory results only. The Java `LearningCandidateLifecyclePolicy` remains the
authority for `APPROVED`, `PROMOTED`, and `ROLLED_BACK` transitions. Canary is
false unless a separately implemented canary evaluator supplies evidence.

Ragas' documented `EvaluationDataset` and `evaluate` APIs are used by the
lazy adapter; model/embedding clients are injected when the worker is wired to
local Ollama or another approved evaluation provider.
