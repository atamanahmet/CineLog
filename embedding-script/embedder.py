"""Turn catalog text into float16 embedding bytes."""

from __future__ import annotations

import logging
import time
from typing import Any

import numpy as np

MODEL_NAME = "BAAI/bge-small-en-v1.5"
EMBEDDING_MODEL_ID = 1
BATCH_SIZE = 64

logger = logging.getLogger(__name__)


def load_model():
    """Load the sentence transformer. The import is slow, so it stays here."""
    from sentence_transformers import SentenceTransformer

    return SentenceTransformer(MODEL_NAME)


def build_text(title: str, overview: str) -> str:
    """Build the text that is embedded for one title."""
    return f"{title}. {overview}"


def to_float16_bytes(vector: Any) -> bytes:
    """Convert one vector to float16 bytes."""
    return np.asarray(vector, dtype=np.float16).tobytes()


def encode_texts(model, texts: list[str]) -> list[bytes]:
    """Encode texts and return one float16 byte string per text."""
    started = time.monotonic()
    embeddings = model.encode(
        texts,
        batch_size=BATCH_SIZE,
        normalize_embeddings=True,
    )
    logger.info("encode duration_s=%.3f", time.monotonic() - started)
    return [to_float16_bytes(row) for row in embeddings]
