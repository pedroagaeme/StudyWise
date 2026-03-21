import base64
import io
import json
import os
import re
from urllib import parse
from urllib import error, request

from youtube_transcript_api import YouTubeTranscriptApi


# ==============================
# Configuration
# ==============================

ALLOWED_DIFFICULTY = {"easy", "medium", "hard"}
ALLOWED_SIZE = {"small", "medium", "large"}
SIZE_TO_COUNT_RANGE = {
    "small": "1-10",
    "medium": "10-20",
    "large": "20-50",
}

DEFAULT_OPENROUTER_MODEL = "z-ai/glm-4.5-air:free"
OPENROUTER_ENDPOINT = "https://openrouter.ai/api/v1/chat/completions"

MAX_LINK_BYTES = 25 * 1024 * 1024
OCR_MODEL_DEFAULT = "google/gemini-2.0-flash-lite-001"


# ==============================
# Request Utilities
# ==============================

def _get_header(headers, name):
    if not isinstance(headers, dict):
        return ""

    target = name.lower()
    for key, value in headers.items():
        if str(key).lower() == target:
            return str(value)
    return ""


def _parse_content_disposition(value):
    result = {}
    if not value:
        return result

    parts = [part.strip() for part in str(value).split(";") if part.strip()]
    for part in parts[1:]:
        if "=" not in part:
            continue
        key, raw_val = part.split("=", 1)
        key = key.strip().lower()
        raw_val = raw_val.strip().strip('"')
        result[key] = raw_val
    return result


def _split_multipart(body_bytes, boundary):
    delimiter = b"--" + boundary
    chunks = body_bytes.split(delimiter)
    parts = []

    for chunk in chunks:
        stripped = chunk.strip()
        if not stripped or stripped == b"--":
            continue

        if stripped.endswith(b"--"):
            stripped = stripped[:-2].rstrip()

        header_blob, sep, payload = stripped.partition(b"\r\n\r\n")
        if not sep:
            header_blob, sep, payload = stripped.partition(b"\n\n")
            if not sep:
                continue

        headers = {}
        header_lines = re.split(rb"\r?\n", header_blob)
        for line in header_lines:
            if b":" not in line:
                continue
            key, value = line.split(b":", 1)
            headers[key.decode("utf-8", errors="ignore").strip().lower()] = value.decode(
                "utf-8", errors="ignore"
            ).strip()

        payload = payload.rstrip(b"\r\n")
        parts.append({"headers": headers, "content": payload})

    return parts


def _extend_links(target, text):
    raw = str(text or "").strip()
    if not raw:
        return

    try:
        parsed = json.loads(raw)
        if isinstance(parsed, list):
            for item in parsed:
                item_text = str(item).strip()
                if item_text:
                    target.append(item_text)
            return
        if isinstance(parsed, str) and parsed.strip():
            target.append(parsed.strip())
            return
    except json.JSONDecodeError:
        pass

    for token in re.split(r"[\n,]", raw):
        link = token.strip()
        if link:
            target.append(link)


def _parse_multipart_body(context):
    headers = getattr(context.req, "headers", {}) or {}
    content_type = _get_header(headers, "content-type")
    match = re.search(r"boundary=(.+)$", content_type)
    if not match:
        raise ValueError("multipart/form-data request is missing boundary")

    boundary = match.group(1).strip().strip('"').encode("utf-8", errors="ignore")
    if not boundary:
        raise ValueError("multipart/form-data request has invalid boundary")

    raw_body = getattr(context.req, "body", b"")
    if isinstance(raw_body, str):
        body_bytes = raw_body.encode("utf-8", errors="ignore")
    elif isinstance(raw_body, (bytes, bytearray)):
        body_bytes = bytes(raw_body)
    else:
        raise ValueError("multipart/form-data body is missing")

    parts = _split_multipart(body_bytes, boundary)
    payload = {
        "difficulty": "",
        "size": "",
        "quiz_summary": "",
        "documents": {"links": [], "files": []},
    }

    for part in parts:
        part_headers = part["headers"]
        disposition = _parse_content_disposition(part_headers.get("content-disposition", ""))
        field_name = disposition.get("name", "").strip()
        filename = disposition.get("filename", "").strip()
        part_content_type = part_headers.get("content-type", "application/octet-stream")
        content = part["content"]

        if filename:
            payload["documents"]["files"].append(
                {
                    "name": filename,
                    "content": base64.b64encode(content).decode("utf-8"),
                    "encoding": "base64",
                    "content_type": part_content_type,
                }
            )
            continue

        text_value = content.decode("utf-8", errors="ignore").strip()
        lowered = field_name.lower()

        if lowered in {"difficulty", "size", "quiz_summary"}:
            payload[lowered] = text_value
            continue

        if lowered in {"links", "links[]", "documents.links", "documents.links[]"}:
            _extend_links(payload["documents"]["links"], text_value)
            continue

        if lowered in {"documents"} and text_value:
            try:
                docs_value = json.loads(text_value)
                if isinstance(docs_value, dict):
                    links = docs_value.get("links", [])
                    files = docs_value.get("files", [])
                    for link in links:
                        link_text = str(link).strip()
                        if link_text:
                            payload["documents"]["links"].append(link_text)
                    if isinstance(files, list):
                        for file_item in files:
                            if isinstance(file_item, dict):
                                payload["documents"]["files"].append(file_item)
            except json.JSONDecodeError:
                pass

    return payload


def json_error(context, status_code, message):
    context.res.status_code(status_code)
    return context.res.json({"error": message})


def extract_json_body(context):
    headers = getattr(context.req, "headers", {}) or {}
    content_type = _get_header(headers, "content-type").lower()

    if "multipart/form-data" in content_type:
        return _parse_multipart_body(context)

    if isinstance(getattr(context.req, "body_json", None), dict):
        return context.req.body_json

    body = getattr(context.req, "body", None)
    if isinstance(body, dict):
        return body

    if isinstance(body, (bytes, bytearray)):
        body = body.decode("utf-8", errors="ignore")

    if isinstance(body, str) and body.strip():
        return json.loads(body)

    raise ValueError("Request body must be valid JSON")


# ==============================
# Document Parsing and OCR
# ==============================

def _strip_html(content):
    without_script = re.sub(r"<script[^>]*>.*?</script>", " ", content, flags=re.S | re.I)
    without_style = re.sub(r"<style[^>]*>.*?</style>", " ", without_script, flags=re.S | re.I)
    text = re.sub(r"<[^>]+>", " ", without_style)
    text = re.sub(r"\s+", " ", text)
    return text.strip()


def _fetch_link_bytes(url, timeout=10, max_bytes=MAX_LINK_BYTES):
    req = request.Request(
        url,
        headers={
            "User-Agent": "StudyWiseQuizGenerator/1.0",
            "Accept": "*/*",
        },
    )

    with request.urlopen(req, timeout=timeout) as response:
        content_type = (response.headers.get("Content-Type") or "").lower()
        raw = response.read(max_bytes + 1)

    if len(raw) > max_bytes:
        raise ValueError("Link content exceeds maximum supported size")

    return raw, content_type


def _guess_content_type_from_name(name):
    lower_name = str(name or "").lower()
    if lower_name.endswith((".png", ".jpg", ".jpeg", ".webp", ".bmp", ".gif", ".tiff")):
        return "image/*"
    if lower_name.endswith((".mp4", ".mov", ".avi", ".mkv", ".webm", ".m4v")):
        return "video/*"
    if lower_name.endswith(".pdf"):
        return "application/pdf"
    return ""


def _is_image_content(content_type):
    return str(content_type or "").lower().startswith("image/")


def _is_video_content(content_type):
    return str(content_type or "").lower().startswith("video/")


def _is_pdf_content(content_type):
    return "application/pdf" in str(content_type or "").lower()


def _looks_like_pdf_url(url):
    try:
        path = parse.urlparse(url).path.lower()
    except ValueError:
        return False
    return path.endswith(".pdf")


def _resolve_ocr_model():
    model = str(os.environ.get("OCR_MODEL", "")).strip()
    if model:
        return model
    return OCR_MODEL_DEFAULT


def _call_gemini_ocr(media_bytes, mime_type, media_kind="image"):
    api_key = str(os.environ.get("OPENROUTER_API_KEY", "")).strip().strip('"').strip("'")
    if not api_key:
        raise RuntimeError("Missing OPENROUTER_API_KEY environment variable")

    encoded = base64.b64encode(media_bytes).decode("ascii")
    safe_mime = str(mime_type or "application/octet-stream").strip().lower()

    if media_kind == "video":
        if not safe_mime.startswith("video/"):
            safe_mime = "video/mp4"
        ocr_text = "Extract all readable text from this video. Return plain text only."
        media_part = {
            "type": "file",
            "file": {
                "filename": "upload-video",
                "file_data": f"data:{safe_mime};base64,{encoded}",
            },
        }
    else:
        if not safe_mime.startswith("image/"):
            safe_mime = "image/png"
        ocr_text = "Extract all readable text from this image. Return plain text only."
        media_part = {
            "type": "image_url",
            "image_url": {"url": f"data:{safe_mime};base64,{encoded}"},
        }

    payload = {
        "model": _resolve_ocr_model(),
        "messages": [
            {
                "role": "user",
                "content": [
                    {
                        "type": "text",
                        "text": ocr_text,
                    },
                    media_part,
                ],
            }
        ],
        "temperature": 0,
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
        with request.urlopen(req, timeout=45) as response:
            raw = response.read().decode("utf-8", errors="ignore")
            parsed = json.loads(raw)
    except (error.URLError, error.HTTPError, TimeoutError, json.JSONDecodeError) as err:
        raise RuntimeError("OCR API request failed") from err

    choices = parsed.get("choices") if isinstance(parsed, dict) else None
    if not isinstance(choices, list) or not choices:
        return ""

    message = choices[0].get("message") if isinstance(choices[0], dict) else {}
    content = message.get("content") if isinstance(message, dict) else ""

    if isinstance(content, str):
        return content.strip()

    if isinstance(content, list):
        parts = []
        for part in content:
            if isinstance(part, dict):
                text = str(part.get("text", "")).strip()
                if text:
                    parts.append(text)
        return "\n".join(parts).strip()

    return ""


def _extract_text_from_image_bytes(image_bytes, mime_type="image/png"):
    try:
        from PIL import Image
    except Exception as err:
        raise RuntimeError("Image processing dependencies are not installed") from err

    try:
        image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
        normalized = io.BytesIO()
        image.save(normalized, format="PNG")
        normalized_bytes = normalized.getvalue()
    except Exception as err:
        raise ValueError("Invalid image bytes") from err

    raw_text = _call_gemini_ocr(normalized_bytes, mime_type, media_kind="image")
    lines = [line.strip() for line in str(raw_text).splitlines() if line.strip()]
    if not lines:
        return ""
    return "\n".join(lines)


def _extract_text_from_video_bytes(video_bytes, mime_type="video/mp4"):
    raw_text = _call_gemini_ocr(video_bytes, mime_type, media_kind="video")
    lines = [line.strip() for line in str(raw_text).splitlines() if line.strip()]
    if not lines:
        return ""
    return "\n".join(lines)


def _extract_text_from_pdf_bytes(pdf_bytes):
    try:
        from pypdf import PdfReader
    except Exception as err:
        raise RuntimeError("PDF parser library is not installed") from err

    reader = PdfReader(io.BytesIO(pdf_bytes))
    pages = []
    for page in reader.pages:
        page_text = page.extract_text() or ""
        page_text = page_text.strip()
        if page_text:
            pages.append(page_text)

    if not pages:
        return ""
    return "\n".join(pages)


def _extract_youtube_video_id(url):
    try:
        parsed = parse.urlparse(url)
    except ValueError:
        return ""

    host = (parsed.netloc or "").lower()
    path = parsed.path or ""

    if "youtu.be" in host:
        return path.strip("/")

    if "youtube.com" in host:
        if path == "/watch":
            qs = parse.parse_qs(parsed.query or "")
            return (qs.get("v") or [""])[0]
        if path.startswith("/shorts/"):
            return path.split("/shorts/", 1)[1].split("/", 1)[0]
        if path.startswith("/embed/"):
            return path.split("/embed/", 1)[1].split("/", 1)[0]

    return ""


def _fetch_youtube_transcript(url):
    video_id = _extract_youtube_video_id(url)
    if not video_id:
        raise ValueError("Invalid YouTube URL: missing video id")

    try:
        api = YouTubeTranscriptApi()
        transcript = api.fetch(video_id, languages=["en", "en-US", "en-GB"])
    except Exception as err:
        raise ValueError("No transcript available for this YouTube URL") from err

    lines = []
    for chunk in transcript:
        if isinstance(chunk, dict):
            text = str(chunk.get("text", "")).strip()
        else:
            text = str(getattr(chunk, "text", "")).strip()
        if text:
            lines.append(text)

    if not lines:
        raise ValueError("No transcript text available for this YouTube URL")

    return " ".join(lines)


def _normalize_documents(documents):
    parsed_documents = []

    if documents is None:
        return parsed_documents

    if isinstance(documents, dict):
        files = documents.get("files", [])
        links = documents.get("links", [])

        for item in links:
            if isinstance(item, str):
                parsed_documents.append({"type": "link", "url": item})
            elif isinstance(item, dict):
                parsed_documents.append({"type": "link", "url": item.get("url", "")})

        for item in files:
            if isinstance(item, dict):
                parsed_documents.append(
                    {
                        "type": "file",
                        "name": item.get("name", "unnamed"),
                        "content": item.get("content", ""),
                        "encoding": item.get("encoding", "utf-8"),
                        "content_type": item.get("content_type", ""),
                    }
                )
            elif isinstance(item, str):
                parsed_documents.append(
                    {
                        "type": "file",
                        "name": "inline-text",
                        "content": item,
                        "encoding": "utf-8",
                        "content_type": "",
                    }
                )

        return parsed_documents

    if isinstance(documents, list):
        for item in documents:
            if isinstance(item, str):
                if item.startswith("http://") or item.startswith("https://"):
                    parsed_documents.append({"type": "link", "url": item})
                else:
                    parsed_documents.append(
                        {
                            "type": "file",
                            "name": "inline-text",
                            "content": item,
                            "encoding": "utf-8",
                            "content_type": "",
                        }
                    )
            elif isinstance(item, dict):
                if item.get("url"):
                    parsed_documents.append({"type": "link", "url": item.get("url")})
                else:
                    parsed_documents.append(
                        {
                            "type": "file",
                            "name": item.get("name", "unnamed"),
                            "content": item.get("content", ""),
                            "encoding": item.get("encoding", "utf-8"),
                            "content_type": item.get("content_type", ""),
                        }
                    )

    return parsed_documents


def _extract_text_from_link(url):
    if _extract_youtube_video_id(url):
        return _fetch_youtube_transcript(url)

    raw, content_type = _fetch_link_bytes(url)
    if _is_pdf_content(content_type) or _looks_like_pdf_url(url):
        return _extract_text_from_pdf_bytes(raw)
    if _is_image_content(content_type):
        return _extract_text_from_image_bytes(raw, content_type)
    if _is_video_content(content_type):
        return _extract_text_from_video_bytes(raw, content_type)

    text = raw.decode("utf-8", errors="ignore")
    if "text/html" in content_type:
        return _strip_html(text)
    return text


def _extract_text_from_file(item, context):
    name = item.get("name", "unnamed")
    raw_content = item.get("content", "")
    encoding = str(item.get("encoding", "utf-8")).lower()
    content_type = str(item.get("content_type", "")).lower() or _guess_content_type_from_name(name)

    if encoding != "base64":
        return str(raw_content)

    try:
        decoded_bytes = base64.b64decode(raw_content)
        if _is_pdf_content(content_type):
            return _extract_text_from_pdf_bytes(decoded_bytes)
        if _is_image_content(content_type):
            return _extract_text_from_image_bytes(decoded_bytes, content_type)
        if _is_video_content(content_type):
            return _extract_text_from_video_bytes(decoded_bytes, content_type)
        return decoded_bytes.decode("utf-8", errors="ignore")
    except (ValueError, TypeError) as decode_err:
        context.error(f"Failed to decode base64 file '{name}': {repr(decode_err)}")
        return ""
    except RuntimeError as ocr_err:
        context.error(f"OCR failed for file '{name}': {repr(ocr_err)}")
        return ""


def prepare_document_context(context, documents):
    normalized = _normalize_documents(documents)
    if not normalized:
        return "No external documents were provided.", False

    chunks = []
    total_chars = 0
    max_total_chars = 60000
    max_item_chars = 12000

    for item in normalized:
        if total_chars >= max_total_chars:
            break

        if item["type"] == "link":
            url = item.get("url", "").strip()
            if not url:
                continue

            try:
                link_text = _extract_text_from_link(url)
                snippet = link_text[:max_item_chars]
                chunk = f"[LINK] {url}\\n{snippet}"
            except (RuntimeError, ValueError, error.URLError, error.HTTPError, TimeoutError) as fetch_err:
                context.error(f"Unable to fetch link '{url}': {repr(fetch_err)}")
                chunk = f"[LINK] {url}\\nFailed to fetch this link."
        else:
            name = item.get("name", "unnamed")
            raw_text = _extract_text_from_file(item, context)
            snippet = raw_text[:max_item_chars]
            chunk = f"[FILE] {name}\\n{snippet}"

        remaining = max_total_chars - total_chars
        if len(chunk) > remaining:
            chunk = chunk[:remaining]

        total_chars += len(chunk)
        chunks.append(chunk)

    if not chunks:
        return "No readable content could be extracted from provided documents.", False
    return "\\n\\n".join(chunks), True


# ==============================
# Prompt Builder
# ==============================

def build_prompt(difficulty, size, quiz_summary, documents_context, has_source_material):
    question_range = SIZE_TO_COUNT_RANGE[size]
    prompt = (
        "You are an expert quiz generator. Build a quiz strictly from the information below.\n\n"
        f"Difficulty: {difficulty}\n"
        f"Quiz size: {size} ({question_range} questions)\n"
        f"Quiz summary: {quiz_summary}\n\n"
        "Source materials:\n"
        f"{documents_context}\n\n"
        "Output instructions:\n"
        "1) Return JSON only (no markdown).\n"
        "2) JSON schema: {\"suggested_name\": string, \"suggested_subject\": string, \"difficulty\": string, \"questions\": Question[]}\n"
        "3) Question schema: {\"type\": \"multiple_choice\", \"question\": string, \"options\": string[4], \"correct_answer\": int, \"explanation\": string}\n"
        "4) Number of questions must match requested size range.\n"
        "5) Focus on the subject matter and meaning of the content, not its formatting or document structure.\n"
        "6) Ignore file-format artifacts (for example PDF headers, page numbers, table-of-contents markers, and layout labels).\n"
    )

    if has_source_material:
        prompt += (
            "7) Keep questions coherent with the supplied material.\n"
            "8) Do not include content not inferable from the provided source materials.\n"
        )
    else:
        prompt += (
            "7) No source files or links were provided, so use general knowledge to create a coherent quiz from the summary.\n"
        )

    return prompt


# ==============================
# OpenRouter Client
# ==============================

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


# ==============================
# Quiz Output Parsing
# ==============================

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


# ==============================
# Appwrite Entrypoint
# ==============================

def main(context):
    try:
        body = extract_json_body(context)
    except (ValueError, json.JSONDecodeError) as err:
        return json_error(context, 400, f"Invalid JSON body: {err}")

    difficulty = str(body.get("difficulty", "")).strip().lower()
    size = str(body.get("size", "")).strip().lower()
    quiz_summary = str(body.get("quiz_summary", "")).strip()
    documents = body.get("documents")

    if difficulty not in ALLOWED_DIFFICULTY:
        return json_error(context, 400, "difficulty must be one of: easy, medium, hard")
    if size not in ALLOWED_SIZE:
        return json_error(context, 400, "size must be one of: small, medium, large")
    if not quiz_summary:
        return json_error(context, 400, "quiz_summary is required")

    documents_context, has_source_material = prepare_document_context(context, documents)
    prompt = build_prompt(difficulty, size, quiz_summary, documents_context, has_source_material)

    try:
        llm_response = call_openrouter(prompt)
        generated_text = extract_model_text(llm_response)

        if not generated_text:
            return json_error(context, 502, "OpenRouter returned an empty response")

        quiz = parse_quiz(generated_text)
        return context.res.json({"quiz": quiz})
    except RuntimeError as runtime_err:
        context.error(f"OpenRouter request failed: {repr(runtime_err)}")
        if str(runtime_err).startswith("OpenRouter API"):
            return json_error(context, 502, "Quiz generation failed. Please try again.")
        return json_error(context, 500, "Internal server error")
