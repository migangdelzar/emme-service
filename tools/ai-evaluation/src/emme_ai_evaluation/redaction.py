import re


_EMAIL = re.compile(
    r"(?i)(?<![A-Za-z0-9._%+-])[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}(?![A-Za-z0-9.-])"
)
_PHONE = re.compile(r"(?<!\d)(?:\+?\d[\d ()-]{7,}\d)(?!\d)")
_BEARER = re.compile(r"(?i)\bBearer\s+\S+")


def redact_text(value: str) -> str:
    """Redact common identifiers before values enter evaluation or logs."""

    if not isinstance(value, str):
        raise TypeError("evaluation text must be a string")
    return _BEARER.sub("[REDACTED_TOKEN]", _PHONE.sub("[REDACTED_PHONE]", _EMAIL.sub("[REDACTED_EMAIL]", value)))
