import math
from collections.abc import Mapping
from typing import Sequence

from .contracts import EvaluationSample


class RagasMetricEvaluator:
    """Lazy adapter around Ragas so unit tests do not require model services."""

    def __init__(self, metrics=None, llm=None, embeddings=None):
        self._metrics = metrics
        self._llm = llm
        self._embeddings = embeddings

    def evaluate(self, samples: Sequence[EvaluationSample]) -> dict[str, float]:
        from ragas import EvaluationDataset, evaluate

        dataset = EvaluationDataset.from_list(
            [
                {
                    "user_input": sample.user_input,
                    "response": sample.response,
                    "retrieved_contexts": list(sample.retrieved_contexts),
                    **(
                        {"reference": sample.reference}
                        if sample.reference is not None
                        else {}
                    ),
                }
                for sample in samples
            ]
        )
        result = evaluate(
            dataset=dataset,
            **({"metrics": self._metrics} if self._metrics is not None else {}),
            **({"llm": self._llm} if self._llm is not None else {}),
            **(
                {"embeddings": self._embeddings}
                if self._embeddings is not None
                else {}
            ),
        )
        if isinstance(result, Mapping):
            return {str(name): float(value) for name, value in result.items()}

        frame = result.to_pandas()
        columns = frame.to_dict(orient="list")
        scores = {}
        for name, values in columns.items():
            numeric_values = [
                float(value)
                for value in values
                if isinstance(value, (int, float)) and math.isfinite(float(value))
            ]
            if numeric_values:
                scores[str(name)] = sum(numeric_values) / len(numeric_values)
        return scores
