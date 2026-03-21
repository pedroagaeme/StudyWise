# Quiz Generator Function

This Appwrite function generates multiple-choice quizzes from source material. It accepts a JSON body and fetches files from Appwrite Storage using file IDs.

## What It Does

- Accepts quiz settings (`difficulty`, `size`, `quiz_summary`)
- Accepts source references via:
  - Appwrite Storage file IDs
  - optional web links
- Downloads files from your bucket and extracts content from:
  - PDFs
  - Images (Gemini OCR)
  - Videos (Gemini OCR)
  - Plain text files
  - YouTube links (transcripts)
- Sends consolidated context to the LLM and returns quiz JSON

## Request Format

This function no longer accepts multipart form-data. Use JSON body only.

### Body Example (Minimal)

```json
{
  "difficulty": "medium",
  "size": "small",
  "quiz_summary": "Cell biology basics",
  "documents": ["FILE_ID_1", "FILE_ID_2"]
}
```

### Body Example (With Links + IDs)

```json
{
  "difficulty": "hard",
  "size": "medium",
  "quiz_summary": "Focus on mechanisms and definitions",
  "documents": {
    "ids": ["FILE_ID_1", "FILE_ID_2"],
    "links": ["https://en.wikipedia.org/wiki/Cell_(biology)"]
  }
}
```

## Response

```json
{
  "quiz": {
    "suggested_name": "Cell Biology Essentials",
    "suggested_subject": "Biology",
    "difficulty": "medium",
    "questions": [
      {
        "type": "multiple_choice",
        "question": "What is the primary function of mitochondria?",
        "options": ["Energy production", "Protein synthesis", "DNA replication", "Waste disposal"],
        "correct_answer": 0,
        "explanation": "Mitochondria produce ATP through cellular respiration."
      }
    ]
  }
}
```

## Configuration

| Setting | Value |
| --- | --- |
| Runtime | Python (3.9+) |
| Entrypoint | `main.py` |
| Build Command | `pip install -r requirements.txt` |
| Timeout | 60 seconds |

## Required Environment Variables

- `OPENROUTER_API_KEY`: OpenRouter API key
- `APPWRITE_ENDPOINT` or `APPWRITE_FUNCTION_API_ENDPOINT`: Appwrite endpoint
- `APPWRITE_PROJECT_ID` or `APPWRITE_FUNCTION_PROJECT_ID`: Appwrite project ID
- `APPWRITE_API_KEY` or `APPWRITE_FUNCTION_API_KEY`: Appwrite API key with Storage read access
- `APPWRITE_BUCKET_ID` or `APPWRITE_STORAGE_BUCKET_ID`: Bucket containing source files

## Optional Environment Variables

- `OPENROUTER_MODEL`: Quiz generation model (default: `z-ai/glm-4.5-air:free`)
- `OCR_MODEL`: OCR vision model (default: `google/gemini-2.0-flash-lite-001`)

