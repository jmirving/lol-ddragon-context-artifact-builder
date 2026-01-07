# Temporary Decisions Doc: DDragon MVP Future Proofing

Goal: list the minimum open questions to answer so the DDragon snapshot + champion mapping MVP is ready to execute without blocking future externalization.

## Storage contract (must be stable)
- What is the canonical path contract for snapshots (example: `ddragon/snapshots/<version>/data/<locale>/champion.json`)?
    Answer: `ddragon/snapshots/<version>/data/<locale>/champion.json` for raw snapshots. Derived artifacts live under `ddragon/artifacts/<artifact_name>/<version-or-latest>.json`.
- Will all files follow the same root layout (for both `champion.json` and other assets)?
    Answer: Yes. All snapshot files live under `ddragon/snapshots/<version>/data/<locale>/...` so new assets can be added without changing consumer paths.
- Are versions immutable forever, or can a version be replaced?
    Answer: Versions are immutable. "Latest" is a separate pointer (for example, `ddragon/manifest.json` or `ddragon/artifacts/<artifact_name>/latest.json`) and never overwrites an existing version.

## Access and configuration
- What is the single configurable base root/URI (`SNAPSHOT_BASE_URI`) that all consumers use?
    Answer: `SNAPSHOT_BASE_URI` (example: `file:///.../data` for MVP, `https://storage.example.com` later). All consumers resolve paths relative to this base.
- Will future access be HTTP-based (preferred) or SDK-only?
    Answer: HTTP-based. Keep clients URL-driven so local and object storage both work.
- How will consumers discover the latest version: `manifest.json` vs hard-coded latest?
    Answer: For MVP, no manifest. Consumers read an explicit `SNAPSHOT_VERSION` config (manual). A manifest is optional later if auto-discovery is needed.

## Publish and atomicity
- Is there a publish step separate from ingest (recommended)?
    Answer: Yes. Ingest writes raw snapshots; a separate job produces derived artifacts.
- How do we ensure atomic publish (write new version, then update `manifest.json`)?
    Answer: MVP is local-only. Write artifacts to a new filename and then swap a `latest` pointer/file if needed. No manifest.
- Do we need an audit log for published versions?
    Answer: Not for MVP.

## Visibility and security
- Public read vs authenticated read in the future?
    Answer: MVP local only. Future externalization likely public read for raw snapshots, with optional auth for derived artifacts if needed.
- If authenticated, what mechanism is acceptable (signed URLs, API proxy)?
    Answer: Defer. If needed later, use signed URLs or an API proxy.

## Retention and lifecycle
- Are old versions retained indefinitely for reproducibility?
    Answer: Yes for MVP; keep all snapshot versions.
- If not, what is the retention window?
    Answer: TBD later if storage costs force retention limits.

## Format and compression
- Primary artifact format for MVP (JSON vs CSV)?
    Answer: JSON for MVP.
- Should we publish compressed files (`.json.gz`) now or later?
    Answer: Later if storage or bandwidth becomes a problem.

## Locale scope
- Which locales are required for MVP (en_US only vs all locales)?
    Answer: en_US only for MVP.
- Should the mapping artifact depend on a single locale for names?
    Answer: Yes, en_US for now.

## Champion mapping artifact
- Confirm MVP schema: `normalized_name,name,id,key` (DDragon-sourced).
- Final normalization rules (lowercase, remove punctuation/spaces, strip diacritics).
0.- Do we need an alias table for non-canonical inputs (optional for MVP)?
- Output format and location relative to snapshots (same bucket/root or separate)?
- Update cadence: regenerate on every snapshot vs on demand?
    Answer: Schema confirmed. Normalization is lowercase + strip punctuation/spaces + remove diacritics. Alias table optional; not required for MVP.
    Answer: Output JSON under `ddragon/artifacts/champion-mapping/<version-or-latest>.json`.
    Answer: Regenerate when a new snapshot version is ingested (manual trigger OK for MVP).

## Consumer contract
- DraftSage should read from base URI and path contract, not local paths.
- Should ChatLoL consume the same mapping artifact or use a separate alias layer?
    Answer: DraftSage reads via `SNAPSHOT_BASE_URI` + version + locale.
    Answer: ChatLoL can use the same mapping artifact and optionally add an alias layer for fuzzy inputs later.

## Ownership and operations
- Which service/job owns artifact generation (separate from the DDragon cron)?
- Where are failures and data drift tracked?
    Answer: Separate artifact generation job (not in the DDragon cron).
    Answer: For MVP, local logs; revisit monitoring when externalized.
