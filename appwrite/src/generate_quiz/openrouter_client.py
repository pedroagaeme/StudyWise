import json
import os
from urllib import error, request

from appwrite.src.generate_quiz.config import DEFAULT_OPENROUTER_MODEL, OPENROUTER_ENDPOINT


def _normalize_openrouter_model(raw_model):
    model = str(raw_model or "").strip()
    if not model:
        return DEFAULT_OPENROUTER_MODEL
    if model.startswith("openrouter/"):
        return model.split("openrouter/", 1)[1]
    return model


def _read_http_error_body(http_error):
    try:
        details = http_error.read().decode("utf-8", errors="ignore")
        return details.strip()
    except Exception:
        return ""


def call_openrouter(prompt):
    api_key = os.environ.get("OPENROUTER_API_KEY", "")
    api_key = api_key.strip().strip('"').strip("'")
    if not api_key:
        raise RuntimeError("Missing OPENROUTER_API_KEY environment variable")

    payload = {
        "model": _normalize_openrouter_model(os.environ.get("OPENROUTER_MODEL")),
        "messages": [{"role": "user", "content": prompt}],
        "temperature": 0.4,
    }

    req = request.Request(
        OPENROUTER_ENDPOINT,
        method="POST",
        data=json.dumps(payload).encode("utf-8"),
    )
    req.add_header("Content-Type", "application/json")
    req.add_header("HTTP-Referer", "https://studywise.local")
    req.add_header("X-Title", "StudyWise")
    req.add_unredirected_header("Authorization", f"Bearer {api_key}")

    try:
        with request.urlopen(req, timeout=30) as response:
            raw = response.read().decode("utf-8", errors="ignore")
            parsed = json.loads(raw)
            parsed["_resolved_model"] = payload["model"]
            return parsed
    except error.HTTPError as http_err:
        details = _read_http_error_body(http_err)
        raise RuntimeError(f"OpenRouter API HTTP {http_err.code}. {details}")
    except error.URLError as url_err:
        raise RuntimeError(f"OpenRouter API network error: {repr(url_err)}")
    except TimeoutError as timeout_err:
        raise RuntimeError(f"OpenRouter API timeout: {repr(timeout_err)}")


def extract_model_text(llm_response):
    choices = llm_response.get("choices", [])
    if not choices:
        return ""

    content = choices[0].get("message", {}).get("content", "")
    if isinstance(content, str):
        return content.strip()

    if isinstance(content, list):
        chunks = []
        for part in content:
            if isinstance(part, dict) and part.get("type") == "text":
                chunks.append(str(part.get("text", "")))
        return "".join(chunks).strip()

    return str(content).strip()
