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
OCR_VIDEO_FRAME_STEP = 30
OCR_MAX_VIDEO_FRAMES = 12
