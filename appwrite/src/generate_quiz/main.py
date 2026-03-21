import json

from config import ALLOWED_DIFFICULTY, ALLOWED_SIZE
from document_parser import prepare_document_context
from openrouter_client import call_openrouter, extract_model_text
from prompt_builder import build_prompt
from quiz_parser import parse_quiz
from request_utils import extract_json_body, json_error


# This Appwrite function will be executed every time your function is triggered
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
