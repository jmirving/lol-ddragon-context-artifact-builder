# lol-ddragon-context-artifact-builder

Generate normalized artifacts from DDragon snapshots produced by `lol-ddragon-snapshot-cron`.

## Scope (MVP)
- Build a champion mapping artifact with schema `normalized_name,name,id,key`.
- Build minimal CSV artifacts for champion core data and champion spell data.
- Normalization rules: lowercase, remove punctuation/spaces, strip diacritics.
- Read snapshots from the existing cron layout: `data/ddragon/extracted/<version>/data/<locale>/champion.json`.
- Also read per-champion payloads from `data/ddragon/extracted/<version>/data/<locale>/champion/*.json`.
- Write artifacts under `ddragon/artifacts/` with a non-versioned default (`latest`).

## Usage

```bash
./gradlew run --args="--snapshot-version 14.1.1 --snapshot-locale en_US"
```

This original standalone invocation and output layout remain supported. For a
worker-style invocation against an ephemeral snapshot and staging directory:

```bash
./gradlew run --args="--snapshot-input /work/snapshot --snapshot-version 14.1.1 --snapshot-locale en_US --output-directory /work/output --artifact-version 14.1.1 --structured-output json"
```

`--snapshot-input` may identify a snapshot root, its locale directory, or its
`champion.json` file. `--output-directory` writes `champion-mapping.json`,
`champion-core.csv`, and `champion-spells.csv` directly into the supplied
directory. Both options accept local paths and `file://` URIs.

`--structured-output json` makes stdout contain only the generic command-adapter
success envelope. Its worker-owned `metadata` includes the snapshot version,
locale, artifact version, and an absolute path, SHA-256 checksum, and byte size
for every artifact. Failures remain non-zero process exits and are reported on
stderr. The builder does not publish or copy artifacts into consumer repos.

### Configuration
All options can be set by flags or environment variables.

- `SNAPSHOT_BASE_URI` (default: `data/ddragon/extracted`)
- `SNAPSHOT_INPUT` (optional direct snapshot root, locale directory, or `champion.json`)
- `SNAPSHOT_VERSION` (required if not passed as `--snapshot-version`)
- `SNAPSHOT_LOCALE` (default: `en_US`)
- `ARTIFACTS_BASE_URI` (default: `data`)
- `OUTPUT_DIRECTORY` (optional direct output directory)
- `ARTIFACT_VERSION` (default: `latest`)
- `STRUCTURED_OUTPUT` (optional; the only supported value is `json`)

Output path example:
`data/ddragon/artifacts/champion-mapping/latest.json`

Additional CSV outputs:
- `data/ddragon/artifacts/champion-core/latest.csv`
- `data/ddragon/artifacts/champion-spells/latest.csv`

## Notes
- Base URIs can be local paths or `file://` URIs.
- Files are replaced atomically where the filesystem supports it, and generated
  content uses stable ordering and LF line endings for repeatable checksums.
- The `normalized_name` field is the canonical join key for downstream consumers.
- The builder tolerates both `<version>/data/...` and `<version>/<version>/data/...` snapshot roots.
- Repository-local code, tests, documentation, and GitHub issues define the current path and artifact contracts.
