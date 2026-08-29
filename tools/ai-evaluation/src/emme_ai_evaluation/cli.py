import argparse
import json
from pathlib import Path

from .pipeline import EvaluationPipeline
from .ragas_adapter import RagasMetricEvaluator


def main() -> None:
    parser = argparse.ArgumentParser(description="Evaluate redacted Emme AI traces with Ragas")
    parser.add_argument("--dataset", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()

    report = EvaluationPipeline(RagasMetricEvaluator()).evaluate_jsonl(args.dataset)
    args.output.write_text(
        json.dumps(
            {
                "sample_count": report.sample_count,
                "metrics": dict(report.metrics),
                "gates": {
                    "dataset_complete": report.gates.dataset_complete,
                    "safety_passed": report.gates.safety_passed,
                    "regression_passed": report.gates.regression_passed,
                    "shadow_comparison_passed": report.gates.shadow_comparison_passed,
                    "canary_passed": report.gates.canary_passed,
                },
                "promotion": "not_performed",
            },
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )


if __name__ == "__main__":
    main()
