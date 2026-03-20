import json
import re
import base64


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
