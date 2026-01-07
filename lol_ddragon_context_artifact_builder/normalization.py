"""Normalization utilities for DDragon artifacts."""

from __future__ import annotations

import unicodedata


def normalize_name(name: str) -> str:
    """Normalize champion names for stable joins."""
    decomposed = unicodedata.normalize("NFKD", name)
    stripped = "".join(ch for ch in decomposed if not unicodedata.combining(ch))
    lowered = stripped.lower()
    return "".join(ch for ch in lowered if ch.isalnum())
