# lol-ddragon-context-artifact-builder

Generate normalized artifacts from DDragon snapshots produced by `lol-ddragon-snapshot-cron`.

## Scope (MVP)
- Build a champion mapping artifact with schema `normalized_name,name,id,key`.
- Normalization rules: lowercase, remove punctuation/spaces, strip diacritics.
- Read snapshots from the existing cron layout: `data/ddragon/extracted/<version>/data/<locale>/champion.json`.
- Write artifacts under `ddragon/artifacts/` with a non-versioned default (`latest.json`).

## Usage

```
./gradlew run --args="--snapshot-version 14.1.1 --snapshot-locale en_US"
```

### Configuration
All options can be set by flags or environment variables.

- `SNAPSHOT_BASE_URI` (default: `data/ddragon/extracted`)
- `SNAPSHOT_VERSION` (required if not passed as `--snapshot-version`)
- `SNAPSHOT_LOCALE` (default: `en_US`)
- `ARTIFACTS_BASE_URI` (default: `data`)
- `ARTIFACT_VERSION` (default: `latest`)

Output path example:
`data/ddragon/artifacts/champion-mapping/latest.json`

## Notes
- Base URIs can be local paths or `file://` URIs.
- The `normalized_name` field is the canonical join key for downstream consumers.
