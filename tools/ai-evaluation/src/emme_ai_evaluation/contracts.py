from dataclasses import dataclass
from typing import Mapping


@dataclass(frozen=True)
class EvaluationSample:
    """PII-redacted fields accepted by the offline evaluator."""

    user_input: str
    response: str
    retrieved_contexts: tuple[str, ...]
    reference: str | None = None


@dataclass(frozen=True)
class EvaluationGates:
    """Advisory gate results; Java remains authoritative for promotion."""

    dataset_complete: bool
    safety_passed: bool
    regression_passed: bool
    shadow_comparison_passed: bool
    canary_passed: bool = False


@dataclass(frozen=True)
class EvaluationReport:
    metrics: Mapping[str, float]
    gates: EvaluationGates
    sample_count: int
