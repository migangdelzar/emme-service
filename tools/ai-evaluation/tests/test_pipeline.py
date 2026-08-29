import json
import sys
import types
import unittest
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest.mock import patch

from emme_ai_evaluation.pipeline import EvaluationPipeline
from emme_ai_evaluation.redaction import redact_text
from emme_ai_evaluation.ragas_adapter import RagasMetricEvaluator


class FakeMetricEvaluator:
    def __init__(self, metrics):
        self.metrics = metrics
        self.samples = None

    def evaluate(self, samples):
        self.samples = list(samples)
        return self.metrics


class EvaluationPipelineTest(unittest.TestCase):
    def test_redacts_pii_before_the_metric_engine_receives_samples(self):
        evaluator = FakeMetricEvaluator(
            {
                "faithfulness": 0.95,
                "answer_relevancy": 0.90,
                "context_precision": 0.92,
            }
        )
        pipeline = EvaluationPipeline(evaluator)

        report = pipeline.evaluate(
            [
                {
                    "trace_id": "trace-1",
                    "user_input": "Contact maria@example.com",
                    "response": "Call +52 555 123 4567",
                    "retrieved_contexts": ["Bearer secret-token"],
                }
            ]
        )

        self.assertEqual(report.metrics["faithfulness"], 0.95)
        self.assertNotIn("maria@example.com", evaluator.samples[0].user_input)
        self.assertNotIn("555 123 4567", evaluator.samples[0].response)
        self.assertNotIn("secret-token", evaluator.samples[0].retrieved_contexts[0])

    def test_empty_dataset_fails_the_dataset_gate_without_calling_metrics(self):
        evaluator = FakeMetricEvaluator({"faithfulness": 1.0})
        pipeline = EvaluationPipeline(evaluator)

        report = pipeline.evaluate([])

        self.assertFalse(report.gates.dataset_complete)
        self.assertFalse(report.gates.regression_passed)
        self.assertIsNone(evaluator.samples)

    def test_report_is_advisory_and_never_promotes_a_candidate(self):
        evaluator = FakeMetricEvaluator(
            {
                "faithfulness": 0.99,
                "answer_relevancy": 0.99,
                "context_precision": 0.99,
            }
        )

        report = EvaluationPipeline(evaluator).evaluate(
            [
                {
                    "trace_id": "trace-1",
                    "user_input": "What are your services?",
                    "response": "We offer manicures.",
                    "retrieved_contexts": ["Manicures are available."],
                }
            ]
        )

        self.assertFalse(report.gates.shadow_comparison_passed)
        self.assertFalse(report.gates.canary_passed)
        self.assertFalse(hasattr(report, "promoted"))

    def test_jsonl_trace_loader_preserves_only_the_evaluation_fields(self):
        evaluator = FakeMetricEvaluator({"faithfulness": 0.90})
        pipeline = EvaluationPipeline(evaluator)

        with TemporaryDirectory() as directory:
            path = Path(directory) / "traces.jsonl"
            path.write_text(
                json.dumps(
                    {
                        "trace_id": "trace-1",
                        "tenant_id": "tenant-secret",
                        "user_input": "What are your services?",
                        "response": "We offer manicures.",
                        "retrieved_contexts": ["Manicures are available."],
                        "ignored": "must not enter evaluation",
                    }
                )
                + "\n",
                encoding="utf-8",
            )

            pipeline.evaluate_jsonl(path)

        self.assertEqual(len(evaluator.samples), 1)
        self.assertEqual(evaluator.samples[0].user_input, "What are your services?")
        self.assertFalse(hasattr(evaluator.samples[0], "tenant_id"))


class RedactionTest(unittest.TestCase):
    def test_redacts_email_phone_and_bearer_token(self):
        redacted = redact_text("maria@example.com +52 555 123 4567 Bearer abc123")

        self.assertNotIn("maria@example.com", redacted)
        self.assertNotIn("555 123 4567", redacted)
        self.assertNotIn("abc123", redacted)


class RagasAdapterTest(unittest.TestCase):
    def test_normalizes_ragas_evaluation_result_scores(self):
        class FakeDataset:
            @classmethod
            def from_list(cls, rows):
                self_rows[:] = rows
                return object()

        class FakeFrame:
            def to_dict(self, orient):
                if orient != "list":
                    raise AssertionError("Ragas scores must be read as lists")
                return {"faithfulness": [0.8, 1.0], "answer_relevancy": [0.9]}

        class FakeResult:
            def to_pandas(self):
                return FakeFrame()

        self_rows = []

        def fake_evaluate(**kwargs):
            self.assertIsNotNone(kwargs["dataset"])
            return FakeResult()

        fake_ragas = types.SimpleNamespace(
            EvaluationDataset=FakeDataset,
            evaluate=fake_evaluate,
        )
        samples = [
            types.SimpleNamespace(
                user_input="question",
                response="answer",
                retrieved_contexts=("context",),
                reference=None,
            )
        ]

        with patch.dict(sys.modules, {"ragas": fake_ragas}):
            metrics = RagasMetricEvaluator().evaluate(samples)

        self.assertEqual(metrics, {"faithfulness": 0.9, "answer_relevancy": 0.9})
        self.assertEqual(self_rows[0]["user_input"], "question")


if __name__ == "__main__":
    unittest.main()
