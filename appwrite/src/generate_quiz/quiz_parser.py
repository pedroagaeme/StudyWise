import json


def extract_json_from_text(raw_text):
    text = str(raw_text or "").strip()
    if not text:
        return ""

    if text.startswith("```"):
        lines = text.splitlines()
        if lines and lines[0].startswith("```"):
            lines = lines[1:]
        if lines and lines[-1].strip().startswith("```"):
            lines = lines[:-1]
        text = "\n".join(lines).strip()

    start = text.find("{")
    end = text.rfind("}")
    if start != -1 and end != -1 and end > start:
        return text[start : end + 1]

    return text


def parse_quiz(generated_text):
    candidate_json_text = extract_json_from_text(generated_text)
    try:
        return json.loads(candidate_json_text)
    except json.JSONDecodeError:
        return {
            "raw": generated_text,
            "warning": "Model output was not valid JSON; returning raw text.",
        }
