from config import SIZE_TO_COUNT_RANGE


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
