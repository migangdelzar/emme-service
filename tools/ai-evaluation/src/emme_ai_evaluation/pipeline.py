import json
import math
from pathlib import Path
from typing import Mapping, Protocol, Sequence

from .contracts import EvaluationGates, EvaluationReport, EvaluationSample
from .redaction import redact_text


class MetricEvaluator(Protocol):
    def evaluate(self, samples: Sequence[EvaluationSample]) -> Mapping[str, float]:
        """Evaluate already-redacted samples and return bounded metric values."""


class EvaluationPipeline:
    """Builds safe Ragas inputs and produces advisory quality gates."""

    _REGRESSION_THRESHOLDS = {
        "faithfulness": 0.90,
        "answer_relevancy": 0.85,
        "context_precision": 0.85,
    }

    def __init__(self, evaluator: MetricEvaluator):
        self._evaluator = evaluator

    def evaluate(self, traces: Sequence[Mapping[str, object]]) -> EvaluationReport:
        samples = tuple(self._sample(trace) for trace in traces)
        if not samples:
            return EvaluationReport(
                metrics={},
                gates=EvaluationGates(
                    dataset_complete=False,
                    safety_passed=False,
                    regression_passed=False,
                    shadow_comparison_passed=False,
                ),
                sample_count=0,
            )

        metrics = self._validate_metrics(self._evaluator.evaluate(samples))
        regression_passed = all(
            metrics.get(name, -1.0) >= threshold
            for name, threshold in self._REGRESSION_THRESHOLDS.items()
        )
        shadow_passed = metrics.get("shadow_comparison", 0.0) >= 1.0
        return EvaluationReport(
            metrics=metrics,
            gates=EvaluationGates(
                dataset_complete=True,
                safety_passed=True,
                regression_passed=regression_passed,
                shadow_comparison_passed=shadow_passed,
            ),
            sample_count=len(samples),
        )

    def evaluate_jsonl(self, path: Path) -> EvaluationReport:
        traces = []
        for line_number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            if not line.strip():
                continue
            try:
                trace = json.loads(line)
            except json.JSONDecodeError as error:
                raise ValueError(f"invalid JSON on line {line_number}") from error
            if not isinstance(trace, dict):
                raise ValueError(f"trace on line {line_number} must be an object")
            traces.append(trace)
        return self.evaluate(traces)

    @staticmethod
    def _sample(trace: Mapping[str, object]) -> EvaluationSample:
        return EvaluationSample(
            user_input=redact_text(EvaluationPipeline._required_text(trace, "user_input")),
            response=redact_text(EvaluationPipeline._required_text(trace, "response")),
            retrieved_contexts=tuple(
                redact_text(value)
                for value in EvaluationPipeline._required_text_list(
                    trace, "retrieved_contexts"
                )
            ),
            reference=(
                redact_text(trace["reference"])
                if isinstance(trace.get("reference"), str)
                else None
            ),
        )

    @staticmethod
    def _required_text(trace: Mapping[str, object], field: str) -> str:
        value = trace.get(field)
        if not isinstance(value, str) or not value.strip():
            raise ValueError(f"{field} must be a non-empty string")
        return value

    @staticmethod
    def _required_text_list(trace: Mapping[str, object], field: str) -> list[str]:
        value = trace.get(field)
        if not isinstance(value, list) or any(
            not isinstance(item, str) or not item.strip() for item in value
        ):
            raise ValueError(f"{field} must be a list of non-empty strings")
        return value

    @staticmethod
    def _validate_metrics(metrics: Mapping[str, float]) -> dict[str, float]:
        validated = {}
        for name, value in metrics.items():
            if not isinstance(name, str) or not isinstance(value, (int, float)):
                raise ValueError("metric names and values must be numeric mappings")
            numeric_value = float(value)
            if not math.isfinite(numeric_value) or not 0.0 <= numeric_value <= 1.0:
                raise ValueError(f"metric {name} must be between 0 and 1")
            validated[name] = numeric_value
        return validated
