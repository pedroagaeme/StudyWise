import base64
import io
import json
import os
import re
import time
import uuid
from urllib import parse
from urllib import error, request

from appwrite.client import Client
from appwrite.services.storage import Storage
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
_APPWRITE_STORAGE = None


def _ctx_log(context, message):
    line = f"[generate_quiz] {message}"
    try:
        if context is not None and hasattr(context, "log"):
            context.log(line)
            return
    except Exception:
        pass

    try:
        print(line)
    except Exception:
        pass


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


def json_error(context, status_code, message):
    context.res.status_code = status_code
    return context.res.json({"error": message})


def extract_json_body(context):
    headers = getattr(context.req, "headers", {}) or {}
    content_type = _get_header(headers, "content-type").lower()

    if "multipart/form-data" in content_type:
        raise ValueError("multipart/form-data is not supported; use JSON body with Appwrite document IDs")

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


def _first_env(*names):
    for name in names:
        value = str(os.environ.get(name, "")).strip()
        if value:
            return value
    return ""

def _get_input_documents_bucket_id():
    bucket_id = _first_env("APPWRITE_INPUT_DOCUMENTS_BUCKET_ID")
    if not bucket_id:
        raise RuntimeError("Missing APPWRITE_INPUT_DOCUMENTS_BUCKET_ID environment variable")
    return bucket_id

def _get_quiz_responses_bucket_id():
    bucket_id = _first_env("APPWRITE_QUIZ_RESPONSES_BUCKET_ID")
    if not bucket_id:
        raise RuntimeError("Missing APPWRITE_QUIZ_RESPONSES_BUCKET_ID environment variable")
    return bucket_id


def _get_appwrite_storage():
    global _APPWRITE_STORAGE
    if _APPWRITE_STORAGE is not None:
        return _APPWRITE_STORAGE

    endpoint = _first_env("APPWRITE_ENDPOINT", "APPWRITE_FUNCTION_API_ENDPOINT")
    project_id = _first_env("APPWRITE_PROJECT_ID", "APPWRITE_FUNCTION_PROJECT_ID")
    api_key = _first_env("APPWRITE_API_KEY", "APPWRITE_FUNCTION_API_KEY")

    if not endpoint or not project_id or not api_key:
        raise RuntimeError(
            "Missing Appwrite configuration. Set APPWRITE_ENDPOINT, APPWRITE_PROJECT_ID, and APPWRITE_API_KEY"
        )

    client = Client()
    client.set_endpoint(endpoint)
    client.set_project(project_id)
    client.set_key(api_key)
    _APPWRITE_STORAGE = Storage(client)
    return _APPWRITE_STORAGE


def _fetch_appwrite_file_as_document(file_id):
    cleaned_file_id = str(file_id or "").strip()
    if not cleaned_file_id:
        raise ValueError("Invalid Appwrite document id")

    storage = _get_appwrite_storage()
    bucket_id = _get_input_documents_bucket_id()

    _ctx_log(None, f"[storage] start file_id={cleaned_file_id} bucket_id={bucket_id}")
    t_meta = time.time()

    metadata = storage.get_file(bucket_id=bucket_id, file_id=cleaned_file_id)
    _ctx_log(None, f"[storage] metadata_ok file_id={cleaned_file_id} elapsed={time.time() - t_meta:.2f}s")

    t_download = time.time()
    file_bytes = storage.get_file_download(bucket_id=bucket_id, file_id=cleaned_file_id)
    _ctx_log(None, f"[storage] download_ok file_id={cleaned_file_id} elapsed={time.time() - t_download:.2f}s")

    def _metadata_value(obj, *keys):
        if isinstance(obj, dict):
            for key in keys:
                if key in obj and obj[key] is not None:
                    return obj[key]
            return None

        for key in keys:
            if hasattr(obj, key):
                value = getattr(obj, key)
                if value is not None:
                    return value
        return None

    if hasattr(file_bytes, "read"):
        file_bytes = file_bytes.read()
    if isinstance(file_bytes, str):
        file_bytes = file_bytes.encode("utf-8", errors="ignore")
    if not isinstance(file_bytes, (bytes, bytearray)):
        raise RuntimeError("Appwrite file download returned unsupported data type")

    _ctx_log(None, f"[storage] bytes_ready file_id={cleaned_file_id} bytes={len(file_bytes)}")

    return {
        "type": "file",
        "name": str(_metadata_value(metadata, "name", "file_name") or cleaned_file_id),
        "content": base64.b64encode(bytes(file_bytes)).decode("utf-8"),
        "encoding": "base64",
        "content_type": str(_metadata_value(metadata, "mimeType", "mime_type", "mime") or ""),
    }


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

    _ctx_log(
        None,
        (
            f"[ocr] start kind={media_kind} mime={safe_mime} bytes={len(media_bytes)} "
            f"model={payload['model']}"
        ),
    )

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
        t_ocr = time.time()
        with request.urlopen(req, timeout=45) as response:
            raw = response.read().decode("utf-8", errors="ignore")
            parsed = json.loads(raw)
        _ctx_log(None, f"[ocr] response_ok kind={media_kind} elapsed={time.time() - t_ocr:.2f}s")
    except (error.URLError, error.HTTPError, TimeoutError, json.JSONDecodeError) as err:
        _ctx_log(None, f"[ocr] response_error kind={media_kind} error={repr(err)}")
        raise RuntimeError("OCR API request failed") from err

    choices = parsed.get("choices") if isinstance(parsed, dict) else None
    if not isinstance(choices, list) or not choices:
        _ctx_log(None, f"[ocr] empty_choices kind={media_kind}")
        return ""

    message = choices[0].get("message") if isinstance(choices[0], dict) else {}
    content = message.get("content") if isinstance(message, dict) else ""

    if isinstance(content, str):
        _ctx_log(None, f"[ocr] content_string kind={media_kind} chars={len(content.strip())}")
        return content.strip()

    if isinstance(content, list):
        parts = []
        for part in content:
            if isinstance(part, dict):
                text = str(part.get("text", "")).strip()
                if text:
                    parts.append(text)
        merged = "\n".join(parts).strip()
        _ctx_log(None, f"[ocr] content_list kind={media_kind} chars={len(merged)}")
        return merged

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
        ids = documents.get("ids", [])
        if not isinstance(ids, list):
            ids = [ids]

        for item in links:
            if isinstance(item, str):
                parsed_documents.append({"type": "link", "url": item})
            elif isinstance(item, dict):
                parsed_documents.append({"type": "link", "url": item.get("url", "")})

        for item in ids:
            parsed_documents.append({"type": "appwrite_file_id", "id": str(item).strip()})

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
                    parsed_documents.append({"type": "appwrite_file_id", "id": item.strip()})
            elif isinstance(item, dict):
                if item.get("url"):
                    parsed_documents.append({"type": "link", "url": item.get("url")})
                elif item.get("id"):
                    parsed_documents.append({"type": "appwrite_file_id", "id": str(item.get("id")).strip()})
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
    _ctx_log(context, f"[docs] normalized_count={len(normalized)}")
    if not normalized:
        return "No external documents were provided.", False

    chunks = []
    total_chars = 0
    max_total_chars = 60000
    max_item_chars = 12000

    for item in normalized:
        if total_chars >= max_total_chars:
            break

        _ctx_log(context, f"[docs] processing type={item.get('type')} total_chars={total_chars}")

        if item["type"] == "link":
            url = item.get("url", "").strip()
            if not url:
                continue

            try:
                _ctx_log(context, f"[docs] link_start url={url}")
                link_text = _extract_text_from_link(url)
                _ctx_log(context, f"[docs] link_ok url={url} chars={len(link_text)}")
                snippet = link_text[:max_item_chars]
                chunk = f"[LINK] {url}\\n{snippet}"
            except (RuntimeError, ValueError, error.URLError, error.HTTPError, TimeoutError) as fetch_err:
                context.error(f"Unable to fetch link '{url}': {repr(fetch_err)}")
                chunk = f"[LINK] {url}\\nFailed to fetch this link."
        elif item["type"] == "appwrite_file_id":
            file_id = str(item.get("id", "")).strip()
            if not file_id:
                continue
            try:
                _ctx_log(context, f"[docs] storage_file_start id={file_id}")
                storage_file_item = _fetch_appwrite_file_as_document(file_id)
                name = storage_file_item.get("name", file_id)
                raw_text = _extract_text_from_file(storage_file_item, context)
                _ctx_log(context, f"[docs] storage_file_ok id={file_id} name={name} chars={len(raw_text)}")
                snippet = raw_text[:max_item_chars]
                chunk = f"[FILE] {name}\\n{snippet}"
            except Exception as fetch_err:
                context.error(f"Unable to fetch Appwrite file '{file_id}': {repr(fetch_err)}")
                chunk = f"[FILE] {file_id}\\nFailed to fetch this file from Appwrite Storage."
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
    _ctx_log(context, f"[docs] done chunks={len(chunks)} total_chars={total_chars}")
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
        "2) JSON schema: {\"title\": string, \"quiz_collection\": QuizCollection, \"difficulty\": string, \"questions\": Question[]}\n"
        "3) QuizCollection schema: {\"name\": string}\n"
        "4) Question schema: {\"type\": \"multiple_choice\", \"description\": string, \"answer_options\": AnswerOption[], \"explanation\": string}\n"
        "5) AnswerOption schema: {\"text\": string, \"is_correct\": boolean}\n"
        "6) Number of questions must match requested size range.\n"
        "7) Focus on the subject matter and meaning of the content, not its formatting or document structure.\n"
        "8) Ignore file-format artifacts (for example PDF headers, page numbers, table-of-contents markers, and layout labels).\n"
    )

    if has_source_material:
        prompt += (
            "9) Keep questions coherent with the supplied material.\n"
            "10) Do not include content not inferable from the provided source materials.\n"
        )
    else:
        prompt += (
            "9) No source files or links were provided, so use general knowledge to create a coherent quiz from the summary.\n"
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


def call_openrouter(prompt, context=None):
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

    _ctx_log(context, f"[llm] start model={payload['model']} prompt_chars={len(prompt)}")

    try:
        t_llm = time.time()
        with request.urlopen(req, timeout=30) as response:
            raw = response.read().decode("utf-8", errors="ignore")
            parsed = json.loads(raw)
            parsed["_resolved_model"] = payload["model"]
            _ctx_log(context, f"[llm] response_ok elapsed={time.time() - t_llm:.2f}s")
            return parsed
    except error.HTTPError as http_err:
        details = _read_http_error_body(http_err)
        _ctx_log(context, f"[llm] response_http_error code={http_err.code}")
        raise RuntimeError(f"OpenRouter API HTTP {http_err.code}. {details}")
    except error.URLError as url_err:
        _ctx_log(context, f"[llm] response_network_error error={repr(url_err)}")
        raise RuntimeError(f"OpenRouter API network error: {repr(url_err)}")
    except TimeoutError as timeout_err:
        _ctx_log(context, f"[llm] response_timeout error={repr(timeout_err)}")
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
    request_id = str(uuid.uuid4())[:8]
    _ctx_log(context, f"[{request_id}] request_start")

    try:
        body = extract_json_body(context)
    except (ValueError, json.JSONDecodeError) as err:
        return json_error(context, 400, f"Invalid JSON body: {err}")

    difficulty = str(body.get("difficulty", "")).strip().lower()
    size = str(body.get("size", "")).strip().lower()
    quiz_summary = str(body.get("quiz_summary", "")).strip()
    documents = body.get("documents")
    quiz_response_file_name = str(body.get("quiz_response_file_name", "")).strip()

    _ctx_log(
        context,
        (
            f"[{request_id}] body_ok difficulty={difficulty} size={size} "
            f"summary_chars={len(quiz_summary)}"
        ),
    )

    if difficulty not in ALLOWED_DIFFICULTY:
        return json_error(context, 400, "difficulty must be one of: easy, medium, hard")
    if size not in ALLOWED_SIZE:
        return json_error(context, 400, "size must be one of: small, medium, large")
    if not quiz_summary:
        return json_error(context, 400, "quiz_summary is required")

    _ctx_log(context, f"[{request_id}] prepare_docs_start")
    documents_context, has_source_material = prepare_document_context(context, documents)
    _ctx_log(
        context,
        (
            f"[{request_id}] prepare_docs_ok has_source={has_source_material} "
            f"context_chars={len(documents_context)}"
        ),
    )
    prompt = build_prompt(difficulty, size, quiz_summary, documents_context, has_source_material)
    _ctx_log(context, f"[{request_id}] prompt_built chars={len(prompt)}")

    try:
        llm_response = call_openrouter(prompt, context=context)
        generated_text = extract_model_text(llm_response)
        _ctx_log(context, f"[{request_id}] model_text_ok chars={len(generated_text)}")

        if not generated_text:
            return json_error(context, 502, "OpenRouter returned an empty response")

        quiz = parse_quiz(generated_text)
        _ctx_log(context, f"[{request_id}] response_success")


        # Write quiz JSON to responses bucket using request_id as file name
        import tempfile
        from appwrite.input_file import InputFile
        storage = _get_appwrite_storage()
        responses_bucket_id = _get_quiz_responses_bucket_id()
        quiz_json_bytes = json.dumps({"quiz": quiz}, ensure_ascii=False, indent=2).encode("utf-8")
        if not quiz_response_file_name:
            return json_error(context, 400, "quiz_response_file_name is required in the request body")
        file_name = f"{quiz_response_file_name}.json"
        try:
            with tempfile.NamedTemporaryFile(delete=False, suffix=".json") as tmpf:
                tmpf.write(quiz_json_bytes)
                tmpf.flush()
                tmp_path = tmpf.name
            input_file = InputFile.from_path(tmp_path)
            storage.create_file(
                bucket_id=responses_bucket_id,
                file_id=file_name,
                file=input_file,
            )
            _ctx_log(context, f"[{request_id}] quiz_json_uploaded bucket={responses_bucket_id} file={file_name}")
        except Exception as upload_err:
            context.error(f"Failed to upload quiz JSON: {repr(upload_err)}")
            return json_error(context, 500, "Failed to upload quiz JSON to storage")

        # Optionally, return a minimal response (or nothing)
        return context.res.json({"quiz_file_id": file_name, "bucket_id": responses_bucket_id})
    except RuntimeError as runtime_err:
        context.error(f"OpenRouter request failed: {repr(runtime_err)}")
        if str(runtime_err).startswith("OpenRouter API"):
            return json_error(context, 502, "Quiz generation failed. Please try again.")
        return json_error(context, 500, "Internal server error")
