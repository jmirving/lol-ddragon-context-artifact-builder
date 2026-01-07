"""Champion mapping artifact generation."""

from __future__ import annotations

from typing import Iterable

from .normalization import normalize_name


def build_champion_mapping(champion_payload: dict) -> list[dict]:
    """Build the champion mapping list from DDragon champion.json payload."""
    data = champion_payload.get("data", {})
    mapping = []

    for champ in data.values():
        name = champ["name"]
        mapping.append(
            {
                "normalized_name": normalize_name(name),
                "name": name,
                "id": champ["id"],
                "key": champ["key"],
            }
        )

    mapping.sort(key=lambda entry: entry["normalized_name"])
    return mapping


def validate_mapping(mapping: Iterable[dict]) -> None:
    """Basic validation to ensure normalized_name is present and unique."""
    seen = set()
    for entry in mapping:
        normalized = entry.get("normalized_name")
        if not normalized:
            raise ValueError("Missing normalized_name in champion mapping entry.")
        if normalized in seen:
            raise ValueError(f"Duplicate normalized_name detected: {normalized}")
        seen.add(normalized)
