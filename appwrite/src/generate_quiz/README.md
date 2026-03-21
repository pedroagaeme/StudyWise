# 📝 Quiz Generator Function

An Appwrite function that intelligently generates quiz questions from various document sources including images, videos, PDFs, YouTube videos, and text. The function extracts semantic content and generates structurally sound quizzes with customizable difficulty and size.

## 🧰 Usage

### POST / (Quiz Generation)

Generates a quiz from provided documents.

**Request Example (cURL)**

```bash
curl -X POST https://your-appwrite-function-url \
  -F "difficulty=medium" \
  -F "size=small" \
  -F "quiz_summary=Optional context or instructions" \
  -F "links=https://example.com" \
  -F "file=@/path/to/document.pdf"
```

**Form Fields**:
- `difficulty` (required): `easy`, `medium`, or `hard`
- `size` (required): `small`, `medium`, or `large`
- `quiz_summary` (optional): Context or specific instructions for quiz generation
- `links` (optional): URLs to fetch content from (can be repeated with multiple `-F` flags)
- `file` (optional): Files to process (images, videos, PDFs, text) - can be repeated with multiple `-F` flags

**Response**

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
        "explanation": "Mitochondria are often called the powerhouse of the cell because they produce ATP through cellular respiration."
      },
      {
        "type": "multiple_choice",
        "question": "Which organelle contains the cell's genetic material?",
        "options": ["Mitochondria", "Nucleus", "Ribosome", "Golgi apparatus"],
        "correct_answer": 1,
        "explanation": "The nucleus contains DNA and controls all cellular activities through gene regulation."
      }
    ]
  }
}
```

**Response Fields**:
- `suggested_name`: Recommended quiz title based on content
- `suggested_subject`: Inferred subject matter from documents
- `difficulty`: The difficulty level requested (`easy`, `medium`, `hard`)
- `questions`: Array of multiple-choice questions with:
  - `type`: Always `"multiple_choice"`
  - `question`: The quiz question
  - `options`: Array of 4 answer options
  - `correct_answer`: Index of the correct option (0-3)
  - `explanation`: Explanation of the correct answer

## Supported Document Types

- **Images**: Encodes image bytes and extracts text using Gemini OCR
- **Videos**: Encodes video bytes and extracts text using Gemini OCR
- **PDFs**: Extracts text while ignoring structural artifacts
- **YouTube**: Fetches video transcripts
- **Text**: Processes plain text directly
- **URLs**: Fetches and processes web page content

## ⚙️ Configuration

| Setting           | Value                             |
| ----------------- | --------------------------------- |
| Runtime           | Python (3.9+)                    |
| Entrypoint        | `main.py`                        |
| Build Commands    | `pip install -r requirements.txt` |
| Permissions       | `any`                             |
| Timeout (Seconds) | 60                                |

## 🔒 Environment Variables

- `OPENROUTER_API_KEY` (required) - API key for OpenRouter LLM service
- `OPENROUTER_MODEL` (optional) - LLM model to use (defaults to `z-ai/glm-4.5-air:free`)
- `OCR_MODEL` (optional) - OCR vision model (defaults to `google/gemini-2.0-flash-lite-001`)
