import base64
import io
import os
import re
import tempfile
from urllib import parse
from urllib import error, request

from youtube_transcript_api import YouTubeTranscriptApi

from appwrite.src.generate_quiz.config import MAX_LINK_BYTES, OCR_MAX_VIDEO_FRAMES, OCR_VIDEO_FRAME_STEP

_OCR_READER = None


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


def _get_ocr_reader():
    global _OCR_READER
    if _OCR_READER is not None:
        return _OCR_READER

    try:
        import easyocr
    except Exception as err:
        raise RuntimeError("OCR library is not installed") from err

    _OCR_READER = easyocr.Reader(["en"], gpu=False)
    return _OCR_READER


def _extract_text_from_image_bytes(image_bytes):
    try:
        import cv2
        import numpy as np
    except Exception as err:
        raise RuntimeError("Image OCR dependencies are not installed") from err

    image_array = np.frombuffer(image_bytes, dtype=np.uint8)
    image = cv2.imdecode(image_array, cv2.IMREAD_COLOR)
    if image is None:
        raise ValueError("Invalid image bytes")

    reader = _get_ocr_reader()
    result = reader.readtext(image, detail=0)
    lines = [str(line).strip() for line in result if str(line).strip()]
    if not lines:
        return ""
    return "\n".join(lines)


def _extract_text_from_video_bytes(video_bytes):
    try:
        import cv2
    except Exception as err:
        raise RuntimeError("Video OCR dependencies are not installed") from err

    temp_path = ""
    try:
        with tempfile.NamedTemporaryFile(delete=False, suffix=".mp4") as temp_file:
            temp_file.write(video_bytes)
            temp_path = temp_file.name

        capture = cv2.VideoCapture(temp_path)
        if not capture.isOpened():
            raise ValueError("Invalid video bytes")

        reader = _get_ocr_reader()
        frame_index = 0
        extracted = []

        while len(extracted) < OCR_MAX_VIDEO_FRAMES:
            ok, frame = capture.read()
            if not ok:
                break

            if frame_index % OCR_VIDEO_FRAME_STEP == 0:
                frame_lines = reader.readtext(frame, detail=0)
                text = " ".join(str(line).strip() for line in frame_lines if str(line).strip())
                if text:
                    extracted.append(text)

            frame_index += 1

        capture.release()
        return "\n".join(extracted)
    finally:
        if temp_path:
            try:
                os.remove(temp_path)
            except OSError:
                pass


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
        return _extract_text_from_image_bytes(raw)
    if _is_video_content(content_type):
        return _extract_text_from_video_bytes(raw)

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
            return _extract_text_from_image_bytes(decoded_bytes)
        if _is_video_content(content_type):
            return _extract_text_from_video_bytes(decoded_bytes)
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
