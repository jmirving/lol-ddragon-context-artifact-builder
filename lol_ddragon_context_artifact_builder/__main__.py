"""CLI entry point for artifact generation."""

from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
from urllib.parse import urlparse

from .champion_mapping import build_champion_mapping, validate_mapping


def resolve_base_uri(uri: str) -> Path:
    if uri.startswith("file://"):
        return Path(urlparse(uri).path)
    return Path(uri)


def load_json(path: Path) -> dict:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def write_json(path: Path, payload: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as handle:
        json.dump(payload, handle, indent=2, ensure_ascii=True)
        handle.write("\n")


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Generate DDragon context artifacts from snapshot data."
    )
    parser.add_argument(
        "--snapshot-base-uri",
        default=os.getenv("SNAPSHOT_BASE_URI", "data/ddragon/extracted"),
        help="Base URI for DDragon snapshots (default: data/ddragon/extracted).",
    )
    parser.add_argument(
        "--snapshot-version",
        default=os.getenv("SNAPSHOT_VERSION"),
        help="DDragon snapshot version (required if env not set).",
    )
    parser.add_argument(
        "--snapshot-locale",
        default=os.getenv("SNAPSHOT_LOCALE", "en_US"),
        help="DDragon locale (default: en_US).",
    )
    parser.add_argument(
        "--artifacts-base-uri",
        default=os.getenv("ARTIFACTS_BASE_URI", "data"),
        help="Base URI for artifact output (default: data).",
    )
    parser.add_argument(
        "--artifact-version",
        default=os.getenv("ARTIFACT_VERSION", "latest"),
        help="Artifact version label (default: latest).",
    )
    return parser


def main() -> int:
    parser = build_parser()
    args = parser.parse_args()

    if not args.snapshot_version:
        parser.error("snapshot version is required via --snapshot-version or SNAPSHOT_VERSION")

    snapshot_base = resolve_base_uri(args.snapshot_base_uri)
    snapshot_path = (
        snapshot_base
        / args.snapshot_version
        / "data"
        / args.snapshot_locale
        / "champion.json"
    )
    if not snapshot_path.exists():
        raise FileNotFoundError(f"Snapshot not found: {snapshot_path}")

    artifacts_base = resolve_base_uri(args.artifacts_base_uri)
    output_path = (
        artifacts_base
        / "ddragon"
        / "artifacts"
        / "champion-mapping"
        / f"{args.artifact_version}.json"
    )

    payload = load_json(snapshot_path)
    mapping = build_champion_mapping(payload)
    validate_mapping(mapping)
    write_json(output_path, mapping)

    print(f"Wrote champion mapping to {output_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
