# Build dictionary

This toolchain refreshes the icon dictionary assets from Open Food Facts and merges them with the hand-curated PT/EN overlays that capture project-specific fixes and additions.

## Refresh workflow

1. Run `./tools/build-dictionary/build.sh`.
2. Review the diff in:
   - `app/src/main/assets/icons/dictionary-pt.json`
   - `app/src/main/assets/icons/dictionary-en.json`
3. Commit the refreshed assets together with any overlay or mapping changes.

## Files

- `ingest_off.py` downloads the OFF `categories.txt` taxonomy, parses the hierarchy, applies `off_to_bucket.json`, and emits per-language base dictionaries into `.cache/`.
- `off_to_bucket.json` maps canonical OFF category identifiers to the app's icon bucket identifiers.
- `overlays/pt.overlay.json` and `overlays/en.overlay.json` contain hand-curated corrections and additions. Overlay entries always win over OFF on key conflicts.
- `build.sh` runs the ingestion step, merges base + overlay deterministically, and writes the final runtime assets.

## Notes

- Runtime only reads `app/src/main/assets/icons/dictionary-{lang}.json`.
- `.cache/` is disposable and is intentionally ignored.
- Output is sorted and formatted deterministically so re-running the build does not create spurious diffs.
